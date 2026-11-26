package com.example.demo.payment.momo;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * HMAC-SHA256 signatures over MoMo's raw strings. Each raw string lists the documented keys in a–z order,
 * exactly as written in the MoMo docs (one-time payment, payment notification and query pages).
 */
public final class MomoSigner {

    private static final String HMAC_SHA256 = "HmacSHA256";

    private final String accessKey;
    private final String secretKey;

    public MomoSigner(String accessKey, String secretKey) {
        this.accessKey = accessKey;
        this.secretKey = secretKey;
    }

    /** Raw string for POST /v2/gateway/api/create. */
    public String createRawSignature(String partnerCode, long amount, String extraData, String ipnUrl, String orderId,
            String orderInfo, String redirectUrl, String requestId, String requestType) {
        return "accessKey=" + accessKey
                + "&amount=" + amount
                + "&extraData=" + extraData
                + "&ipnUrl=" + ipnUrl
                + "&orderId=" + orderId
                + "&orderInfo=" + orderInfo
                + "&partnerCode=" + partnerCode
                + "&redirectUrl=" + redirectUrl
                + "&requestId=" + requestId
                + "&requestType=" + requestType;
    }

    /** Raw string used to verify an IPN (payment notification). */
    public String ipnRawSignature(MomoIpnRequest ipn) {
        return "accessKey=" + accessKey
                + "&amount=" + ipn.amount()
                + "&extraData=" + nullToEmpty(ipn.extraData())
                + "&message=" + nullToEmpty(ipn.message())
                + "&orderId=" + ipn.orderId()
                + "&orderInfo=" + nullToEmpty(ipn.orderInfo())
                + "&orderType=" + nullToEmpty(ipn.orderType())
                + "&partnerCode=" + ipn.partnerCode()
                + "&payType=" + nullToEmpty(ipn.payType())
                + "&requestId=" + ipn.requestId()
                + "&responseTime=" + ipn.responseTime()
                + "&resultCode=" + ipn.resultCode()
                + "&transId=" + ipn.transId();
    }

    /** Raw string for POST /v2/gateway/api/query. */
    public String queryRawSignature(String partnerCode, String orderId, String requestId) {
        return "accessKey=" + accessKey
                + "&orderId=" + orderId
                + "&partnerCode=" + partnerCode
                + "&requestId=" + requestId;
    }

    public String sign(String rawSignature) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), HMAC_SHA256));
            return HexFormat.of().formatHex(mac.doFinal(rawSignature.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("Cannot compute MoMo signature", e);
        }
    }

    /** Constant-time comparison so the check does not leak how many characters matched. */
    public boolean verify(String rawSignature, String providedSignature) {
        if (providedSignature == null) {
            return false;
        }
        byte[] expected = sign(rawSignature).getBytes(StandardCharsets.US_ASCII);
        byte[] provided = providedSignature.toLowerCase().getBytes(StandardCharsets.US_ASCII);
        return MessageDigest.isEqual(expected, provided);
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
