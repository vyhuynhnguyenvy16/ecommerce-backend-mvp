# Deploy và kiểm thử thanh toán MoMo (sandbox)

## 1. Thứ tự merge

| # | Repo | Branch | Base |
|---|---|---|---|
| 0a | backend | `fix/enable-method-security` | `develop` |
| 0b | backend | `fix/cart-lazy-loading` | `fix/enable-method-security` |
| 1 | backend | `feat/payments-01-ci-test-foundation` | `develop` |
| 2 | backend | `feat/payments-02-momo-create` | PR 1 |
| 3 | backend | `feat/payments-03-ipn` | PR 2 |
| 4 | backend | `feat/payments-04-ledger` | PR 3 |
| 5 | backend | `feat/payments-05-outbox-kafka` | PR 4 |
| 6 | backend | `feat/payments-06-expiry-reconciliation` | PR 5 |
| 7 | backend | `feat/payments-07-production-readiness` | PR 6 |
| 8 | frontend | `feat/momo-checkout` | `main` |

Hai bản sửa 0a/0b độc lập với tính năng thanh toán và nên merge trước. Stack PR 1→7 đã chứa sẵn chúng.
Frontend chỉ merge **sau khi** backend PR 7 đã chạy trên production, vì frontend cần tham số redirect `order` và thời
gian có múi giờ.

Flyway chạy V5→V9 khi app khởi động. Tất cả migration chỉ *thêm* cột/bảng; đơn cũ tự thành COD.

## 2. Biến môi trường trên Railway (service backend)

| Biến | Giá trị |
|---|---|
| `MOMO_ENDPOINT` | `https://test-payment.momo.vn` |
| `MOMO_PARTNER_CODE` | `MOMO` (key test công khai trong code mẫu chính thức github.com/momo-wallet/payment) |
| `MOMO_ACCESS_KEY` | `F8BBA842ECF85` (cùng nguồn) |
| `MOMO_SECRET_KEY` | lấy từ code mẫu Python/Go của repo trên (`collection_link.py`); không commit vào git |
| `MOMO_IPN_URL` | `https://ecommerce-backend-mvp-production.up.railway.app/api/payments/momo/ipn` |
| `MOMO_REDIRECT_URL` | `https://smart-shop-ui.vy-huynhnguyenvy-42b.workers.dev/checkout/momo-return` |
| `CORS_ALLOWED_ORIGINS` | `https://smart-shop-ui.vy-huynhnguyenvy-42b.workers.dev` (thêm origin khác, cách nhau bằng dấu phẩy) |
| `VESTRA_KAFKA_ENABLED` | bỏ trống hoặc `false`; outbox vẫn được ghi, chỉ chưa publish |

Nếu thiếu bất kỳ biến MoMo nào, lựa chọn MoMo trả 409 "Thanh toán MoMo tạm thời chưa khả dụng"; COD vẫn chạy bình
thường.

## 3. Kiểm thử tay trên production (sandbox)

Tài liệu MoMo hướng dẫn thanh toán sandbox bằng **ứng dụng MoMo Test** với **tài khoản MoMo TEST**
(Đăng nhập → Trang chủ → Quét QR), theo hướng dẫn trên các trang API thanh toán của developers.momo.vn.

1. Đăng nhập VESTRA, thêm sản phẩm, vào Thanh toán, chọn **Ví MoMo**, bấm "Thanh toán MoMo".
2. Trang MoMo sandbox mở ra. Quét QR bằng app MoMo Test và xác nhận.
3. Trình duyệt quay về `/checkout/momo-return` → "Thanh toán thành công" trong vài giây.
4. Đơn trong "Tài khoản" có trạng thái Confirmed / Đã thanh toán.
5. Lặp lại nhưng bấm hủy trên MoMo → "Chưa thanh toán được" → "Thanh toán lại" tạo link mới.
6. Lặp lại nhưng không trả, đợi 15 phút → đơn tự hủy và kho được hoàn.
7. Admin: `POST /api/admin/reconciliation/run` rồi `GET /api/admin/reconciliation/items` → không có mục mới.

Nếu bước 3 đứng ở "Đang xác nhận": kiểm tra log Railway tìm `MoMo IPN` / `MoMo gateway error`, và kiểm tra
`MOMO_IPN_URL` là URL public. Job đối soát vẫn tự hỏi MoMo sau 5 phút, nên một IPN bị mất không làm mất tiền.

## 4. Chạy local không cần MoMo thật

`tools/momo-mock/mock_momo.py` giả lập create, query và IPN theo đúng định dạng chữ ký của MoMo, đồng thời **kiểm tra
chữ ký** của request VESTRA gửi đi.

```bash
docker compose up -d postgres redis kafka
python3 tools/momo-mock/mock_momo.py 9090 &
MOMO_ENDPOINT=http://localhost:9090 MOMO_PARTNER_CODE=MOMO MOMO_ACCESS_KEY=F8BBA842ECF85 \
MOMO_SECRET_KEY=<secret của code mẫu> MOMO_IPN_URL=http://localhost:8081/api/payments/momo/ipn \
MOMO_REDIRECT_URL=http://localhost:8080/checkout/momo-return ./mvnw spring-boot:run
```

Trang thanh toán giả có hai nút "Xác nhận thanh toán" / "Từ chối" và cố ý gửi mỗi IPN **hai lần** để thử
idempotency. `GET http://localhost:9090/log` cho xem mock đã nhận/gửi gì.
