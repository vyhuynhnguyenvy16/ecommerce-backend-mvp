package com.example.demo.reconciliation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.example.demo.entity.ProductVariant;
import com.example.demo.outbox.OutboxEvent;
import com.example.demo.outbox.OutboxEventRepository;
import com.example.demo.payment.PaymentAttempt;
import com.example.demo.payment.PaymentAttemptRepository;
import com.example.demo.payment.PaymentEvent;
import com.example.demo.payment.PaymentExpiryService;
import com.example.demo.payment.PaymentRepository;
import com.example.demo.payment.PaymentResult;
import com.example.demo.payment.PaymentResultProcessor;
import com.example.demo.payment.PaymentStatus;
import com.example.demo.payment.momo.MomoClient;
import com.example.demo.payment.momo.MomoGatewayException;
import com.example.demo.repository.OrderRepository;
import com.example.demo.service.JwtService;
import com.example.demo.support.AbstractIntegrationTest;
import com.example.demo.support.TestData;
import com.jayway.jsonpath.JsonPath;

class ExpiryAndReconciliationTest extends AbstractIntegrationTest {

    @Autowired
    WebApplicationContext context;
    @Autowired
    TestData data;
    @Autowired
    JdbcTemplate jdbc;
    @Autowired
    PaymentRepository paymentRepository;
    @Autowired
    PaymentAttemptRepository attemptRepository;
    @Autowired
    OrderRepository orderRepository;
    @Autowired
    OutboxEventRepository outbox;
    @Autowired
    PaymentExpiryService expiryService;
    @Autowired
    ReconciliationService reconciliation;
    @Autowired
    PaymentResultProcessor processor;
    @Autowired
    JwtService jwtService;

    @MockitoBean
    MomoClient momoClient;

    MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        when(momoClient.createPayment(anyString(), anyString(), anyLong(), anyString(), anyString()))
                .thenAnswer(inv -> new MomoClient.CreateResponse("MOMO", inv.getArgument(1), inv.getArgument(0),
                        inv.getArgument(2), 1L, "ok", 0, "https://pay/" + inv.getArgument(0), null, null));
    }

    // ---------------------------------------------------------------- expiry

    @Test
    void unpaidOrderIsExpiredCancelledAndRestockedAfterMomoConfirmsNothingWasPaid() throws Exception {
        Fixture f = momoOrderWithAttempt();
        momoSays(f, 1005, null);
        makeDue(f);

        expiryService.expireDuePayments();

        assertThat(paymentStatus(f)).isEqualTo(PaymentStatus.EXPIRED);
        assertThat(orderRepository.findById(f.orderId).orElseThrow().getStatus()).isEqualTo("CANCELLED");
        assertThat(orderRepository.findById(f.orderId).orElseThrow().getPaymentStatus()).isEqualTo("EXPIRED");
        assertThat(data.stockOf(f.variant)).isEqualTo(10);
        assertThat(outbox.findByAggregateTypeAndAggregateIdOrderByIdAsc("Order", String.valueOf(f.orderId)))
                .extracting(OutboxEvent::getEventType).containsExactly("PaymentExpired");
    }

    @Test
    void lostIpnIsRecoveredByTheQueryAndTheOrderIsNotCancelled() throws Exception {
        Fixture f = momoOrderWithAttempt();
        momoSays(f, 0, 5000000001L);
        makeDue(f);

        PaymentExpiryService.Result result = expiryService.expireDuePayments();

        assertThat(result.paidOnQuery()).isGreaterThanOrEqualTo(1);
        assertThat(paymentStatus(f)).isEqualTo(PaymentStatus.PAID);
        assertThat(orderRepository.findById(f.orderId).orElseThrow().getStatus()).isEqualTo("CONFIRMED");
        assertThat(data.stockOf(f.variant)).isEqualTo(8);
        assertThat(jdbc.queryForObject("select count(*) from journal_entries where source_id = 'trans:5000000001'",
                Integer.class)).isEqualTo(1);
    }

    @Test
    void paymentIsLeftOpenWhenMomoCannotBeReached() throws Exception {
        Fixture f = momoOrderWithAttempt();
        when(momoClient.queryPayment(eq(f.providerOrderId), anyString()))
                .thenThrow(new MomoGatewayException("timeout", (Integer) null));
        makeDue(f);

        expiryService.expireDuePayments();

        assertThat(paymentStatus(f)).isEqualTo(PaymentStatus.PENDING);
        assertThat(data.stockOf(f.variant)).isEqualTo(8);
    }

    @Test
    void requestLevelRejectionIsTreatedAsUncertainNotAsAFailedPayment() throws Exception {
        Fixture f = momoOrderWithAttempt();
        momoSays(f, 10, null); // system under maintenance
        makeDue(f);

        expiryService.expireDuePayments();

        assertThat(paymentStatus(f)).isEqualTo(PaymentStatus.PENDING);
    }

    @Test
    void orderIdUnknownToMomoMeansTheLinkWasNeverPaidAndTheOrderExpires() throws Exception {
        Fixture f = momoOrderWithAttempt();
        momoSays(f, 42, null);
        makeDue(f);

        expiryService.expireDuePayments();

        assertThat(paymentStatus(f)).isEqualTo(PaymentStatus.EXPIRED);
    }

    // ---------------------------------------------------------------- reconciliation

    @Test
    void reconciliationRecoversAStalePendingPaymentBeforeItExpires() throws Exception {
        Fixture f = momoOrderWithAttempt();
        momoSays(f, 0, 5000000002L);
        jdbc.update("update payment_attempts set created_at = now() - interval '10 minutes' where provider_order_id = ?",
                f.providerOrderId);

        reconciliation.run("TEST");

        assertThat(paymentStatus(f)).isEqualTo(PaymentStatus.PAID);
    }

    @Test
    void anomaliesBecomeItemsOnceAndAnAdminCanResolveThem() throws Exception {
        Fixture f = momoOrderWithAttempt();
        mvc.perform(patch("/api/orders/" + f.orderId + "/cancel").header("Authorization", f.buyer.bearer()))
                .andExpect(status().isOk());
        processor.apply(new PaymentResult(PaymentEvent.Source.IPN, f.providerOrderId, 80000L, 5000000003L, 0, "{}"));

        reconciliation.run("TEST");
        reconciliation.run("TEST");

        List<Map<String, Object>> items = itemsFor(f.orderId);
        assertThat(items).extracting(i -> i.get("type")).containsExactly("PAID_AFTER_CLOSE");

        Long itemId = ((Number) items.get(0).get("id")).longValue();
        String admin = "Bearer " + jwtService.generateAccessToken(f.buyer.userId(), "ADMIN");
        mvc.perform(patch("/api/admin/reconciliation/items/" + itemId + "/resolve")
                        .header("Authorization", f.buyer.bearer())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"note\":\"x\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(patch("/api/admin/reconciliation/items/" + itemId + "/resolve")
                        .header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"note\":\"Refunded manually on MoMo\"}"))
                .andExpect(status().isNoContent());
        assertThat(jdbc.queryForObject("select status from reconciliation_items where id = ?", String.class, itemId))
                .isEqualTo("RESOLVED");
        mvc.perform(get("/api/admin/reconciliation/items").header("Authorization", admin)).andExpect(status().isOk());
        mvc.perform(post("/api/admin/reconciliation/run").header("Authorization", admin)).andExpect(status().isOk());
    }

    @Test
    void paidPaymentWithoutLedgerEntryAndInconsistentOrderStatusAreFlagged() throws Exception {
        Fixture f = momoOrderWithAttempt();
        // Simulate a bug or a manual DB edit that bypassed the processor.
        jdbc.update("update payments set status = 'PAID', provider_trans_id = 5000000004 where order_id = ?", f.orderId);

        reconciliation.run("TEST");

        assertThat(itemsFor(f.orderId)).extracting(i -> i.get("type"))
                .containsExactlyInAnyOrder("LEDGER_MISSING", "STATUS_MISMATCH");
    }

    // ---------------------------------------------------------------- helpers

    private record Fixture(TestData.Customer buyer, Long orderId, String providerOrderId, ProductVariant variant) {
    }

    private Fixture momoOrderWithAttempt() throws Exception {
        TestData.Customer buyer = data.customer();
        ProductVariant variant = data.variant(new BigDecimal("40000"), 10);
        data.putInCart(buyer, variant, 2);
        String body = mvc.perform(post("/api/orders")
                        .header("Authorization", buyer.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"shippingRecipientName":"A","shippingPhone":"0900000000",
                                 "shippingAddressLine":"1 B","shippingCity":"Hà Nội","paymentMethod":"MOMO"}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long orderId = ((Number) JsonPath.read(body, "$.id")).longValue();
        mvc.perform(post("/api/orders/" + orderId + "/payments/momo").header("Authorization", buyer.bearer()))
                .andExpect(status().isOk());
        PaymentAttempt attempt = attemptRepository.findFirstByPaymentIdOrderByCreatedAtDescIdDesc(
                paymentRepository.findByOrderId(orderId).orElseThrow().getId()).orElseThrow();
        return new Fixture(buyer, orderId, attempt.getProviderOrderId(), variant);
    }

    private void momoSays(Fixture f, int resultCode, Long transId) {
        when(momoClient.queryPayment(eq(f.providerOrderId), anyString()))
                .thenReturn(new MomoClient.QueryResponse("MOMO", "q", f.providerOrderId, 80000L, transId, resultCode,
                        "msg", "qr", 1L));
    }

    private void makeDue(Fixture f) {
        jdbc.update("update payments set expires_at = now() - interval '1 minute' where order_id = ?", f.orderId);
    }

    private PaymentStatus paymentStatus(Fixture f) {
        return paymentRepository.findByOrderId(f.orderId).orElseThrow().getStatus();
    }

    private List<Map<String, Object>> itemsFor(Long orderId) {
        return jdbc.queryForList("select id, type from reconciliation_items where order_id = ? order by id", orderId);
    }
}
