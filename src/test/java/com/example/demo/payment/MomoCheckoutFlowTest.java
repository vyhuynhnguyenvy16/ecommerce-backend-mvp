package com.example.demo.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.example.demo.entity.ProductVariant;
import com.example.demo.payment.momo.MomoClient;
import com.example.demo.support.AbstractIntegrationTest;
import com.example.demo.support.TestData;
import com.jayway.jsonpath.JsonPath;

class MomoCheckoutFlowTest extends AbstractIntegrationTest {

    @Autowired
    WebApplicationContext context;
    @Autowired
    TestData data;
    @Autowired
    PaymentRepository paymentRepository;

    @MockitoBean
    MomoClient momoClient;

    MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        when(momoClient.createPayment(anyString(), anyString(), anyLong(), anyString(), anyString()))
                .thenAnswer(inv -> new MomoClient.CreateResponse("MOMO", inv.getArgument(1), inv.getArgument(0),
                        inv.getArgument(2), 1L, "Thành công", 0,
                        "https://test-payment.momo.vn/v2/gateway/pay?t=" + inv.getArgument(0), null, null));
    }

    @Test
    void momoCheckoutCreatesPendingPaymentAndReusesTheSameLink() throws Exception {
        TestData.Customer buyer = data.customer();
        ProductVariant variant = data.variant(new BigDecimal("75000"), 5);
        data.putInCart(buyer, variant, 2);

        Long orderId = checkout(buyer, "MOMO");

        mvc.perform(get("/api/orders/" + orderId).header("Authorization", buyer.bearer()))
                .andExpect(jsonPath("$.paymentMethod").value("MOMO"))
                .andExpect(jsonPath("$.paymentStatus").value("PENDING"));
        assertThat(data.stockOf(variant)).isEqualTo(3);

        String firstUrl = startMomo(buyer, orderId);
        String secondUrl = startMomo(buyer, orderId);

        assertThat(secondUrl).isEqualTo(firstUrl);
        verify(momoClient, times(1)).createPayment(anyString(), anyString(), eq(150000L), anyString(),
                eq("https://shop.example.com/checkout/momo-return?order=" + orderId));

        mvc.perform(get("/api/orders/" + orderId + "/payment").header("Authorization", buyer.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.method").value("MOMO"))
                .andExpect(jsonPath("$.lastAttemptStatus").value("PENDING"))
                .andExpect(jsonPath("$.canRetry").value(true))
                // Must carry an offset, or a browser in another time zone shows the wrong countdown.
                .andExpect(jsonPath("$.expiresAt").value(matchesPattern(".*([+-]\\d{2}:\\d{2}|Z)$")));
    }

    @Test
    void anotherCustomerCannotStartOrReadSomeoneElsesPayment() throws Exception {
        TestData.Customer buyer = data.customer();
        TestData.Customer stranger = data.customer();
        data.putInCart(buyer, data.variant(new BigDecimal("50000"), 5), 1);
        Long orderId = checkout(buyer, "MOMO");

        mvc.perform(post("/api/orders/" + orderId + "/payments/momo").header("Authorization", stranger.bearer()))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/orders/" + orderId + "/payment").header("Authorization", stranger.bearer()))
                .andExpect(status().isForbidden());
    }

    @Test
    void codCheckoutIsUnchangedAndHasNoPaymentRecord() throws Exception {
        TestData.Customer buyer = data.customer();
        data.putInCart(buyer, data.variant(new BigDecimal("50000"), 5), 1);

        Long orderId = checkout(buyer, null);

        mvc.perform(get("/api/orders/" + orderId).header("Authorization", buyer.bearer()))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.paymentMethod").value("COD"))
                .andExpect(jsonPath("$.paymentStatus").value("NOT_REQUIRED"));
        assertThat(paymentRepository.findByOrderId(orderId)).isEmpty();
        mvc.perform(post("/api/orders/" + orderId + "/payments/momo").header("Authorization", buyer.bearer()))
                .andExpect(status().isConflict());
    }

    @Test
    void momoRejectsAmountsBelowTheGatewayMinimumWithoutTouchingStock() throws Exception {
        TestData.Customer buyer = data.customer();
        ProductVariant cheap = data.variant(new BigDecimal("500"), 5);
        data.putInCart(buyer, cheap, 1);

        mvc.perform(post("/api/orders")
                        .header("Authorization", buyer.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderBody("MOMO")))
                .andExpect(status().isConflict());

        assertThat(data.stockOf(cheap)).isEqualTo(5);
    }

    @Test
    void cancellingAPendingMomoOrderCancelsItsPaymentAndBlocksNewLinks() throws Exception {
        TestData.Customer buyer = data.customer();
        data.putInCart(buyer, data.variant(new BigDecimal("50000"), 5), 1);
        Long orderId = checkout(buyer, "MOMO");

        mvc.perform(patch("/api/orders/" + orderId + "/cancel").header("Authorization", buyer.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentStatus").value("CANCELLED"));

        assertThat(paymentRepository.findByOrderId(orderId).orElseThrow().getStatus())
                .isEqualTo(PaymentStatus.CANCELLED);
        mvc.perform(post("/api/orders/" + orderId + "/payments/momo").header("Authorization", buyer.bearer()))
                .andExpect(status().isConflict());
    }

    @Test
    void storefrontOriginIsAllowedByCorsAndOthersAreNot() throws Exception {
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .options("/api/orders")
                        .header("Origin", "https://smart-shop-ui.vy-huynhnguyenvy-42b.workers.dev")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header()
                        .string("Access-Control-Allow-Origin", "https://smart-shop-ui.vy-huynhnguyenvy-42b.workers.dev"));
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .options("/api/orders")
                        .header("Origin", "https://evil.example.com")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isForbidden());
    }

    private Long checkout(TestData.Customer buyer, String method) throws Exception {
        String body = mvc.perform(post("/api/orders")
                        .header("Authorization", buyer.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderBody(method)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    private String startMomo(TestData.Customer buyer, Long orderId) throws Exception {
        String body = mvc.perform(post("/api/orders/" + orderId + "/payments/momo")
                        .header("Authorization", buyer.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(orderId))
                .andExpect(jsonPath("$.expiresAt").exists())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.payUrl");
    }

    private static String orderBody(String method) {
        String payment = method == null ? "" : ",\"paymentMethod\":\"" + method + "\"";
        return """
                {"shippingRecipientName":"Nguyễn Văn A","shippingPhone":"0900000000",
                 "shippingAddressLine":"268 Lý Thường Kiệt","shippingCity":"TP.HCM"%s}
                """.formatted(payment);
    }
}
