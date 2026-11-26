package com.example.demo.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.example.demo.payment.momo.MomoClient;
import com.example.demo.payment.momo.MomoIpnRequest;
import com.example.demo.payment.momo.MomoSigner;
import com.example.demo.repository.OrderRepository;
import com.example.demo.support.AbstractIntegrationTest;
import com.example.demo.support.TestData;
import com.jayway.jsonpath.JsonPath;

import tools.jackson.databind.json.JsonMapper;

class MomoIpnTest extends AbstractIntegrationTest {

    private static final String IPN = "/api/payments/momo/ipn";

    @Autowired
    WebApplicationContext context;
    @Autowired
    TestData data;
    @Autowired
    MomoSigner signer;
    @Autowired
    JsonMapper jsonMapper;
    @Autowired
    PaymentRepository paymentRepository;
    @Autowired
    PaymentAttemptRepository attemptRepository;
    @Autowired
    PaymentEventRepository eventRepository;
    @Autowired
    OrderRepository orderRepository;
    @Autowired
    PaymentResultProcessor processor;
    @Autowired
    JdbcTemplate jdbc;

    @MockitoBean
    MomoClient momoClient;

    MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        when(momoClient.createPayment(anyString(), anyString(), anyLong(), anyString(), anyString()))
                .thenAnswer(inv -> new MomoClient.CreateResponse("MOMO", inv.getArgument(1), inv.getArgument(0),
                        inv.getArgument(2), 1L, "Thành công", 0,
                        "https://test-payment.momo.vn/pay?t=" + inv.getArgument(0), null, null));
    }

    @Test
    void validSuccessIpnMarksPaymentPaidAndConfirmsOrderWithoutAuthentication() throws Exception {
        Pending p = pendingMomoOrder();

        sendIpn(signed(p, 120000L, 0, 3000000001L)).andExpect(status().isNoContent())
                .andExpect(content().string(""));

        Payment payment = paymentRepository.findByOrderId(p.orderId).orElseThrow();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(payment.getProviderTransId()).isEqualTo(3000000001L);
        assertThat(payment.getPaidAt()).isNotNull();
        mvc.perform(get("/api/orders/" + p.orderId + "/payment").header("Authorization", p.buyer.bearer()))
                .andExpect(jsonPath("$.status").value("PAID"))
                .andExpect(jsonPath("$.orderStatus").value("CONFIRMED"))
                .andExpect(jsonPath("$.canRetry").value(false));
        assertThat(outcomes(p)).containsExactly(PaymentEventOutcome.PAID);
        assertThat(ledgerLines(3000000001L)).containsExactly("1130 DEBIT 120000", "5110 CREDIT 120000");
    }

    @Test
    void sameIpnDeliveredTwiceIsAppliedOnce() throws Exception {
        Pending p = pendingMomoOrder();
        MomoIpnRequest ipn = signed(p, 120000L, 0, 3000000002L);

        sendIpn(ipn).andExpect(status().isNoContent());
        sendIpn(ipn).andExpect(status().isNoContent());

        assertThat(outcomes(p)).containsExactly(PaymentEventOutcome.PAID, PaymentEventOutcome.DUPLICATE_IGNORED);
        assertThat(ledgerLines(3000000002L)).hasSize(2);
    }

    @Test
    void concurrentDuplicateResultsAreAppliedExactlyOnce() throws Exception {
        Pending p = pendingMomoOrder();
        PaymentResult result = new PaymentResult(PaymentEvent.Source.IPN, p.providerOrderId, 120000L, 3000000003L,
                0, "{}");

        ExecutorService pool = Executors.newFixedThreadPool(8);
        try {
            List<Callable<PaymentEventOutcome>> calls = new ArrayList<>();
            for (int i = 0; i < 8; i++) {
                calls.add(() -> processor.apply(result));
            }
            List<PaymentEventOutcome> results = new ArrayList<>();
            for (Future<PaymentEventOutcome> f : pool.invokeAll(calls)) {
                results.add(f.get());
            }
            assertThat(results).filteredOn(o -> o == PaymentEventOutcome.PAID).hasSize(1);
            assertThat(results).filteredOn(o -> o == PaymentEventOutcome.DUPLICATE_IGNORED).hasSize(7);
        } finally {
            pool.shutdown();
        }
        assertThat(paymentRepository.findByOrderId(p.orderId).orElseThrow().getStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(ledgerLines(3000000003L)).containsExactly("1130 DEBIT 120000", "5110 CREDIT 120000");
    }

    @Test
    void invalidSignatureIsRejectedAndChangesNothing() throws Exception {
        Pending p = pendingMomoOrder();
        MomoIpnRequest good = signed(p, 120000L, 0, 3000000004L);
        MomoIpnRequest forged = withSignature(good, "0".repeat(64));

        sendIpn(forged).andExpect(status().isBadRequest());

        assertThat(paymentRepository.findByOrderId(p.orderId).orElseThrow().getStatus())
                .isEqualTo(PaymentStatus.PENDING);
        assertThat(outcomes(p)).containsExactly(PaymentEventOutcome.INVALID_SIGNATURE);
    }

    @Test
    void amountMismatchIsRecordedAsAnomalyAndDoesNotMarkPaid() throws Exception {
        Pending p = pendingMomoOrder();

        sendIpn(signed(p, 1000L, 0, 3000000005L)).andExpect(status().isNoContent());

        assertThat(paymentRepository.findByOrderId(p.orderId).orElseThrow().getStatus())
                .isEqualTo(PaymentStatus.PENDING);
        assertThat(outcomes(p)).containsExactly(PaymentEventOutcome.AMOUNT_MISMATCH);
        assertThat(ledgerLines(3000000005L)).isEmpty();
    }

    @Test
    void failedAttemptKeepsPaymentOpenAndTheCustomerGetsANewLink() throws Exception {
        Pending p = pendingMomoOrder();

        sendIpn(signed(p, 120000L, 1006, 0L)).andExpect(status().isNoContent());

        assertThat(attemptRepository.findByProviderOrderId(p.providerOrderId).orElseThrow().getStatus())
                .isEqualTo(AttemptStatus.FAILED);
        mvc.perform(get("/api/orders/" + p.orderId + "/payment").header("Authorization", p.buyer.bearer()))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.lastAttemptStatus").value("FAILED"))
                .andExpect(jsonPath("$.lastResultCode").value(1006))
                .andExpect(jsonPath("$.canRetry").value(true));

        String retryUrl = startMomo(p);
        assertThat(retryUrl).isNotEqualTo(p.payUrl);
    }

    @Test
    void moneyArrivingAfterCancellationIsFlaggedNotSilentlyAccepted() throws Exception {
        Pending p = pendingMomoOrder();
        mvc.perform(patch("/api/orders/" + p.orderId + "/cancel").header("Authorization", p.buyer.bearer()))
                .andExpect(status().isOk());

        sendIpn(signed(p, 120000L, 0, 3000000006L)).andExpect(status().isNoContent());

        assertThat(paymentRepository.findByOrderId(p.orderId).orElseThrow().getStatus())
                .isEqualTo(PaymentStatus.CANCELLED);
        assertThat(outcomes(p)).containsExactly(PaymentEventOutcome.PAID_AFTER_CLOSE);
        // The money is real, so it is booked, but as owed back to the customer rather than as revenue.
        assertThat(ledgerLines(3000000006L)).containsExactly("1130 DEBIT 120000", "3388 CREDIT 120000");
    }

    @Test
    void aSecondDifferentPaymentForAPaidOrderIsBookedAsRefundPayable() throws Exception {
        Pending p = pendingMomoOrder();
        sendIpn(signed(p, 120000L, 0, 3000000008L)).andExpect(status().isNoContent());

        sendIpn(signed(p, 120000L, 0, 3000000009L)).andExpect(status().isNoContent());

        assertThat(outcomes(p)).containsExactly(PaymentEventOutcome.PAID, PaymentEventOutcome.DUPLICATE_PAYMENT);
        assertThat(ledgerLines(3000000009L)).containsExactly("1130 DEBIT 120000", "3388 CREDIT 120000");
    }

    @Test
    void customerCannotCancelAnOrderThatIsAlreadyPaid() throws Exception {
        Pending p = pendingMomoOrder();
        sendIpn(signed(p, 120000L, 0, 3000000007L)).andExpect(status().isNoContent());

        mvc.perform(patch("/api/orders/" + p.orderId + "/cancel").header("Authorization", p.buyer.bearer()))
                .andExpect(status().isBadRequest());
        assertThat(orderRepository.findById(p.orderId).orElseThrow().getStatus()).isEqualTo("CONFIRMED");
    }

    // ---------------------------------------------------------------------------------------------------------

    private record Pending(TestData.Customer buyer, Long orderId, String providerOrderId, String requestId,
            String payUrl) {
    }

    private Pending pendingMomoOrder() throws Exception {
        TestData.Customer buyer = data.customer();
        data.putInCart(buyer, data.variant(new BigDecimal("60000"), 10), 2);
        String body = mvc.perform(post("/api/orders")
                        .header("Authorization", buyer.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"shippingRecipientName":"A","shippingPhone":"0900000000",
                                 "shippingAddressLine":"1 Đường B","shippingCity":"Huế","paymentMethod":"MOMO"}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long orderId = ((Number) JsonPath.read(body, "$.id")).longValue();
        Pending partial = new Pending(buyer, orderId, null, null, null);
        String payUrl = startMomo(partial);
        PaymentAttempt attempt = attemptRepository.findFirstByPaymentIdOrderByCreatedAtDescIdDesc(
                paymentRepository.findByOrderId(orderId).orElseThrow().getId()).orElseThrow();
        return new Pending(buyer, orderId, attempt.getProviderOrderId(), attempt.getRequestId(), payUrl);
    }

    private String startMomo(Pending p) throws Exception {
        String body = mvc.perform(post("/api/orders/" + p.orderId + "/payments/momo")
                        .header("Authorization", p.buyer.bearer()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.payUrl");
    }

    private MomoIpnRequest signed(Pending p, Long amount, int resultCode, Long transId) {
        MomoIpnRequest unsigned = new MomoIpnRequest("MOMO", p.providerOrderId, p.requestId, amount,
                "VESTRA order #" + p.orderId, "momo_wallet", transId, resultCode, "message", "qr",
                1764150060000L, "", null);
        return withSignature(unsigned, signer.sign(signer.ipnRawSignature(unsigned)));
    }

    private static MomoIpnRequest withSignature(MomoIpnRequest ipn, String signature) {
        return new MomoIpnRequest(ipn.partnerCode(), ipn.orderId(), ipn.requestId(), ipn.amount(), ipn.orderInfo(),
                ipn.orderType(), ipn.transId(), ipn.resultCode(), ipn.message(), ipn.payType(), ipn.responseTime(),
                ipn.extraData(), signature);
    }

    private org.springframework.test.web.servlet.ResultActions sendIpn(MomoIpnRequest ipn) throws Exception {
        return mvc.perform(post(IPN).contentType(MediaType.APPLICATION_JSON).content(jsonMapper.writeValueAsString(ipn)));
    }

    private List<String> ledgerLines(long transId) {
        return jdbc.queryForList("""
                select l.account_code || ' ' || l.direction || ' ' || l.amount
                from journal_lines l join journal_entries e on e.id = l.entry_id
                where e.source_type = 'MOMO_PAYMENT' and e.source_id = ?
                order by l.direction desc, l.account_code
                """, String.class, "trans:" + transId);
    }

    private List<PaymentEventOutcome> outcomes(Pending p) {
        return eventRepository.findByProviderOrderIdOrderByIdAsc(p.providerOrderId).stream()
                .map(PaymentEvent::getOutcome)
                .toList();
    }
}
