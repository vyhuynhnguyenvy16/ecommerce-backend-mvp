package com.example.demo.payment.momo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class MomoClientTest {

    private MockRestServiceServer server;
    private MomoClient client;

    @BeforeEach
    void setUp() {
        MomoProperties properties = new MomoProperties("https://momo.test", "MOMO", MomoSignerTest.ACCESS_KEY,
                MomoSignerTest.SECRET_KEY, "https://api.example.com/api/payments/momo/ipn",
                "https://shop.example.com/checkout/momo-return", "vi");
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new MomoClient(builder, properties, new MomoSigner(properties.accessKey(), properties.secretKey()));
    }

    @Test
    void createSendsDocumentedFieldsWithSignatureAndReturnsPayUrl() {
        server.expect(requestTo("https://momo.test/v2/gateway/api/create"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.partnerCode").value("MOMO"))
                .andExpect(jsonPath("$.requestType").value("captureWallet"))
                .andExpect(jsonPath("$.amount").value(150000))
                .andExpect(jsonPath("$.orderId").value("VESTRA-42-1764150000000"))
                .andExpect(jsonPath("$.ipnUrl").value("https://api.example.com/api/payments/momo/ipn"))
                .andExpect(jsonPath("$.lang").value("vi"))
                .andExpect(jsonPath("$.signature")
                        .value("fce7e6db152c38bc2b6c7c58466f203dde05aed1e3d0338f03947d9ba9cfe375"))
                .andRespond(withSuccess("""
                        {"partnerCode":"MOMO","requestId":"req-0001","orderId":"VESTRA-42-1764150000000",
                         "amount":150000,"responseTime":1764150001000,"message":"Thành công","resultCode":0,
                         "payUrl":"https://test-payment.momo.vn/v2/gateway/pay?t=abc","shortLink":"ignored"}
                        """, MediaType.APPLICATION_JSON));

        MomoClient.CreateResponse response = client.createPayment("VESTRA-42-1764150000000", "req-0001", 150000,
                "VESTRA order #42", "https://shop.example.com/checkout/momo-return?orderId=42");

        assertThat(response.payUrl()).isEqualTo("https://test-payment.momo.vn/v2/gateway/pay?t=abc");
        server.verify();
    }

    @Test
    void createThrowsWhenMomoRejectsTheRequest() {
        server.expect(requestTo("https://momo.test/v2/gateway/api/create"))
                .andRespond(withSuccess("""
                        {"orderId":"VESTRA-42-1","resultCode":41,"message":"Duplicate orderId"}
                        """, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.createPayment("VESTRA-42-1", "req", 150000, "info", "https://r"))
                .isInstanceOf(MomoGatewayException.class)
                .extracting(e -> ((MomoGatewayException) e).getResultCode())
                .isEqualTo(41);
    }

    @Test
    void createThrowsGatewayExceptionOnServerError() {
        server.expect(requestTo("https://momo.test/v2/gateway/api/create")).andRespond(withServerError());

        assertThatThrownBy(() -> client.createPayment("VESTRA-42-1", "req", 150000, "info", "https://r"))
                .isInstanceOf(MomoGatewayException.class);
    }

    @Test
    void querySendsSignatureAndParsesResult() {
        server.expect(requestTo("https://momo.test/v2/gateway/api/query"))
                .andExpect(jsonPath("$.signature")
                        .value("524ab93cfce11bc82ddf44eb4936f9d3f2c85b7087a2bf255f24b6cb7c013db6"))
                .andRespond(withSuccess("""
                        {"partnerCode":"MOMO","orderId":"VESTRA-42-1764150000000","requestId":"req-0002",
                         "amount":150000,"transId":4088878653,"resultCode":0,"message":"Thành công",
                         "payType":"qr","responseTime":1764150070000,"refundTrans":[]}
                        """, MediaType.APPLICATION_JSON));

        MomoClient.QueryResponse response = client.queryPayment("VESTRA-42-1764150000000", "req-0002");

        assertThat(response.resultCode()).isZero();
        assertThat(response.transId()).isEqualTo(4088878653L);
    }
}
