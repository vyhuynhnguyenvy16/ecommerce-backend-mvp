# VESTRA — Đặc tả thanh toán MoMo + sổ cái kép + đối soát + outbox/Kafka

Phiên bản 0.1 · 26/11/2026 · Trạng thái: **chờ duyệt**

## 1. Mục tiêu
Khách hàng VESTRA có thể trả tiền đơn hàng bằng ví MoMo (môi trường sandbox). Hệ thống ghi nhận kết quả an toàn
(xác thực chữ ký, chống xử lý trùng), hạch toán vào sổ cái kép, tự đối soát với MoMo và phát sự kiện qua outbox/Kafka.

## 2. Phạm vi
**Trong phạm vi**
- Chọn phương thức thanh toán khi checkout: `COD` (giữ nguyên) hoặc `MOMO`.
- Tạo link thanh toán MoMo (`POST /v2/gateway/api/create`, `requestType=captureWallet`), cho phép thanh toán lại.
- Nhận IPN tại `POST /api/payments/momo/ipn`: xác thực HMAC-SHA256, đối chiếu `partnerCode`/`orderId`/`amount`,
  xử lý idempotent, trả **HTTP 204** đúng tài liệu MoMo.
- Sổ cái kép (double-entry) của shop, chỉ ghi thêm (append-only), luôn cân Nợ = Có.
- Job hết hạn: đơn MoMo chưa thanh toán sau **15 phút** → hỏi MoMo trước (query API), nếu chưa trả thì huỷ và hoàn kho.
- Job đối soát: giao dịch đang chờ → query MoMo; giao dịch đã PAID ↔ bút toán sổ cái; ghi báo cáo chênh lệch.
- Transactional outbox + Kafka (bật bằng cờ `vestra.kafka.enabled`; local và CI bật, production mặc định tắt).
- Giao diện: chọn phương thức ở checkout, trang kết quả MoMo (tự cập nhật trạng thái), trạng thái thanh toán và nút
  "Thanh toán lại" ở trang đơn hàng.

**Ngoài phạm vi (để sau)**
- Hoàn tiền (refund API), thanh toán 2 bước (`autoCapture=false`), ví khách hàng, màn hình admin đối soát / sổ cái
  (dữ liệu vẫn có, chỉ chưa có UI), microservice riêng.

## 3. Quyết định đã chốt
| Chủ đề | Quyết định |
|---|---|
| Kiến trúc | Module `payment` + `ledger` + `outbox` trong monolith hiện có, ranh giới package rõ ràng |
| Sổ cái | Sổ cái của shop, không có số dư ví khách |
| Kafka | Docker Compose + Testcontainers; production tắt bằng cờ, outbox vẫn được ghi |
| Deploy | Production trên Railway + Cloudflare, dùng sandbox MoMo |
| Key MoMo | Bộ key test công khai trong code mẫu MoMo, nạp qua biến môi trường |
| Hết hạn | 15 phút |
| Hoàn tiền | Để sau |

## 4. Thiết kế backend

### 4.1 Dữ liệu (Flyway V5+, chỉ thêm, không sửa bảng cũ ngoài thêm cột)
- `orders`: thêm `payment_method` (`COD`|`MOMO`, mặc định `COD`), `payment_status`
  (`UNPAID`|`PENDING`|`PAID`|`FAILED`|`EXPIRED`|`NOT_REQUIRED`).
- `payments`: `id`, `order_id`, `provider`, `provider_order_id` (unique, dạng `VESTRA-{orderId}-{attempt}`),
  `request_id`, `amount` (VND, long), `status`, `trans_id` (unique khi có), `result_code`, `pay_url`,
  `expires_at`, `version` (optimistic locking), timestamps.
- `payment_events`: lưu thô mỗi IPN/kết quả query (để kiểm toán), kèm `signature_valid`.
- `ledger_accounts`, `journal_entries` (unique `(source_type, source_id)` → chống hạch toán trùng), `journal_lines`
  (`direction` DEBIT/CREDIT, `amount` > 0).
- `outbox_events` (`aggregate_type`, `aggregate_id`, `event_type`, `payload` jsonb, `published_at`),
  `processed_events` (consumer idempotency).
- `reconciliation_runs`, `reconciliation_items` (loại chênh lệch, trạng thái).

### 4.2 Luồng thanh toán
1. `POST /api/orders/checkout` nhận thêm `paymentMethod`. Với `MOMO`: đơn `PENDING`, `payment_status=PENDING`.
2. `POST /api/orders/{id}/payments/momo` (chủ đơn mới gọi được) → tạo attempt mới, gọi MoMo create, trả `payUrl`
   và `expiresAt`. Số tiền phải trong khoảng 1.000 – 50.000.000 VND (giới hạn của MoMo).
3. Khách trả tiền trên MoMo → MoMo redirect về `{FRONTEND}/checkout/momo-return?orderId=…`.
   **Frontend không tin tham số redirect**; nó gọi `GET /api/orders/{id}/payment` để lấy trạng thái thật.
4. MoMo gọi IPN → xác thực chữ ký → khoá dòng payment → nếu đã ở trạng thái cuối thì bỏ qua (vẫn trả 204) →
   `resultCode=0` thì `PAID` + đơn `CONFIRMED` + bút toán sổ cái + outbox `PaymentSucceeded`, trong **một giao dịch DB**.
   Mã thất bại cuối (1001–1088, 2019, 4001, 4100, 98, 99) thì `FAILED`. Mã chờ (1000, 7000, 7002) thì giữ `PENDING`.
5. Chữ ký sai → 400, không đổi trạng thái, vẫn lưu `payment_events` để điều tra.

### 4.3 Sổ cái kép
- Tài khoản: `1130 Phải thu MoMo` (tài sản), `5110 Doanh thu bán hàng` (doanh thu).
- Thanh toán thành công: **Nợ 1130 / Có 5110** đúng bằng `amount`.
- Bất biến kiểm bằng test: tổng Nợ = tổng Có của mọi bút toán; một payment chỉ có tối đa một bút toán.

### 4.4 Outbox + Kafka
- Bản ghi outbox được ghi cùng giao dịch với thay đổi trạng thái.
- Relay quét `outbox_events` chưa publish (`FOR UPDATE SKIP LOCKED`) và gửi lên topic `vestra.payments`
  khi `vestra.kafka.enabled=true`.
- Consumer mẫu `order-notifications` ghi log thông báo và chống xử lý trùng bằng `processed_events`.

### 4.5 Job định kỳ
- **Hết hạn** (mỗi phút): payment `PENDING` quá `expires_at` → query MoMo. Nếu đã trả thì xử lý như IPN; nếu chưa
  thì `EXPIRED`, huỷ đơn, hoàn kho. Không bao giờ huỷ đơn mà không hỏi MoMo trước.
- **Đối soát** (mỗi 15 phút): (a) payment chờ quá 5 phút → query; (b) payment `PAID` không có bút toán, hoặc bút toán
  lệch số tiền → ghi `reconciliation_items`.

### 4.6 Cấu hình (biến môi trường)
`MOMO_ENDPOINT`, `MOMO_PARTNER_CODE`, `MOMO_ACCESS_KEY`, `MOMO_SECRET_KEY`, `MOMO_IPN_URL`, `MOMO_REDIRECT_URL`,
`VESTRA_KAFKA_ENABLED`, `KAFKA_BOOTSTRAP_SERVERS`.

## 5. Thiết kế giao diện (smart-shop-ui, theo DESIGN.md)
- **Checkout**: nhóm radio "Phương thức thanh toán": COD / Ví MoMo (kèm mô tả "Bạn sẽ được chuyển sang MoMo").
  Đặt hàng với MoMo → gọi tạo link → chuyển trang sang `payUrl`.
- **Trang `/checkout/momo-return`**: 3 trạng thái (đang xác nhận · thành công · thất bại/hết hạn), polling mỗi 2 giây,
  tối đa 60 giây, sau đó cho bấm "Kiểm tra lại". Thông báo có `aria-live`, tôn trọng "giảm chuyển động".
- **Tài khoản → Đơn hàng**: badge trạng thái thanh toán; đơn MoMo đang chờ và chưa hết hạn có nút "Thanh toán lại"
  kèm đếm ngược thời gian còn lại.

## 6. Tiêu chí nghiệm thu
1. Chữ ký tạo request và chữ ký IPN khớp với giá trị kỳ vọng tính độc lập (HMAC-SHA256 theo đúng chuỗi trong tài liệu).
2. IPN chữ ký sai → 400, trạng thái không đổi.
3. Gửi cùng một IPN thành công 2 lần (kể cả song song) → đúng 1 bút toán, 1 sự kiện outbox, đơn `CONFIRMED` một lần.
4. IPN có `amount` lệch → không ghi nhận PAID, sinh mục chênh lệch.
5. Đơn MoMo không trả sau 15 phút → `EXPIRED`, kho được hoàn đúng số lượng; nếu MoMo báo đã trả thì phải là `PAID`.
6. Tổng Nợ = tổng Có trên toàn sổ cái sau mọi kịch bản test.
7. Với Kafka bật: sự kiện `PaymentSucceeded` tới topic đúng một lần với consumer idempotent.
8. Luồng COD hiện tại không đổi hành vi (test hồi quy).
9. CI (GitHub Actions) xanh cho mọi PR; frontend build + lint xanh.
10. Kiểm thử tay trên production: thanh toán bằng app MoMo Test → đơn chuyển "Đã thanh toán".

## 7. Chia PR (mỗi PR một mục đích, PR nối tiếp nhau)
| # | Repo | Nội dung |
|---|---|---|
| 1 | backend | CI GitHub Actions + nền tảng test (Testcontainers Postgres/Redis) + đặc tả này |
| 2 | backend | Phương thức thanh toán + MoMo client/chữ ký + API tạo link + `GET payment` |
| 3 | backend | IPN endpoint: xác thực, idempotent, chuyển trạng thái |
| 4 | backend | Sổ cái kép + hạch toán khi PAID |
| 5 | backend | Outbox + relay Kafka + consumer mẫu (cờ bật/tắt) |
| 6 | backend | Job hết hạn 15 phút + job đối soát |
| 7 | frontend | Checkout chọn MoMo + trang kết quả + trạng thái ở đơn hàng |
| 8 | cả hai | Hướng dẫn deploy, biến môi trường Railway, checklist kiểm thử tay |

Agent không merge PR; chủ repo review và merge theo thứ tự.
