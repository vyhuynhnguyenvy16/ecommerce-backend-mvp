package com.example.demo.payment.momo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Body MoMo POSTs to our ipnUrl (server-to-server). Unknown fields such as promotionInfo are ignored. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record MomoIpnRequest(
        String partnerCode,
        String orderId,
        String requestId,
        Long amount,
        String orderInfo,
        String orderType,
        Long transId,
        Integer resultCode,
        String message,
        String payType,
        Long responseTime,
        String extraData,
        String signature) {
}
