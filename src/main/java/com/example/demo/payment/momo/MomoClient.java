package com.example.demo.payment.momo;

import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Thin client for the MoMo payment gateway (sandbox: https://test-payment.momo.vn).
 * The docs ask for a call timeout of at least 30 seconds on create and query; see {@code MomoConfig}.
 */
public class MomoClient {

    static final String CREATE_PATH = "/v2/gateway/api/create";
    static final String QUERY_PATH = "/v2/gateway/api/query";
    static final String REQUEST_TYPE = "captureWallet";

    private final RestClient restClient;
    private final MomoProperties properties;
    private final MomoSigner signer;

    public MomoClient(RestClient.Builder builder, MomoProperties properties, MomoSigner signer) {
        this.restClient = builder.baseUrl(properties.endpoint()).build();
        this.properties = properties;
        this.signer = signer;
    }

    public CreateResponse createPayment(String orderId, String requestId, long amount, String orderInfo,
            String redirectUrl) {
        String extraData = "";
        String raw = signer.createRawSignature(properties.partnerCode(), amount, extraData, properties.ipnUrl(),
                orderId, orderInfo, redirectUrl, requestId, REQUEST_TYPE);
        CreateRequest body = new CreateRequest(properties.partnerCode(), requestId, amount, orderId, orderInfo,
                redirectUrl, properties.ipnUrl(), REQUEST_TYPE, extraData, lang(), signer.sign(raw));

        CreateResponse response = post(CREATE_PATH, body, CreateResponse.class);
        if (response.resultCode() == null || response.resultCode() != MomoResultCodes.SUCCESS
                || response.payUrl() == null) {
            throw new MomoGatewayException("MoMo create payment rejected: " + response.message(),
                    response.resultCode());
        }
        return response;
    }

    public QueryResponse queryPayment(String orderId, String requestId) {
        String raw = signer.queryRawSignature(properties.partnerCode(), orderId, requestId);
        QueryRequest body = new QueryRequest(properties.partnerCode(), requestId, orderId, lang(), signer.sign(raw));
        QueryResponse response = post(QUERY_PATH, body, QueryResponse.class);
        if (response.resultCode() == null) {
            throw new MomoGatewayException("MoMo query returned no resultCode", (Integer) null);
        }
        return response;
    }

    private <T> T post(String path, Object body, Class<T> type) {
        try {
            T response = restClient.post()
                    .uri(path)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(type);
            if (response == null) {
                throw new MomoGatewayException("Empty response from MoMo " + path, (Integer) null);
            }
            return response;
        } catch (RestClientException e) {
            throw new MomoGatewayException("Cannot reach MoMo " + path, e);
        }
    }

    private String lang() {
        return properties.lang() == null || properties.lang().isBlank() ? "vi" : properties.lang();
    }

    public record CreateRequest(String partnerCode, String requestId, long amount, String orderId, String orderInfo,
            String redirectUrl, String ipnUrl, String requestType, String extraData, String lang,
            String signature) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CreateResponse(String partnerCode, String requestId, String orderId, Long amount,
            Long responseTime, String message, Integer resultCode, String payUrl, String deeplink,
            String qrCodeUrl) {
    }

    public record QueryRequest(String partnerCode, String requestId, String orderId, String lang,
            String signature) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record QueryResponse(String partnerCode, String requestId, String orderId, Long amount, Long transId,
            Integer resultCode, String message, String payType, Long responseTime) {
    }
}
