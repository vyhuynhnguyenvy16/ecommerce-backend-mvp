package com.example.demo.payment.momo;

import java.util.Set;

/**
 * Classification of MoMo resultCode values, from
 * https://developers.momo.vn/v3/vi/docs/payment/api/result-handling/resultcode/
 */
public final class MomoResultCodes {

    public static final int SUCCESS = 0;
    /** Authorised but not captured (2-step payments). VESTRA uses autoCapture=true, so this is not expected. */
    public static final int AUTHORIZED = 9000;

    /** Query: "orderId invalid or not found" — MoMo has no transaction for it (the link was never paid). */
    public static final int ORDER_NOT_FOUND = 42;

    private static final Set<Integer> PENDING = Set.of(1000, 7000, 7002);

    private MomoResultCodes() {
    }

    public static boolean isSuccess(int code) {
        return code == SUCCESS;
    }

    public static boolean isPending(int code) {
        return PENDING.contains(code) || code == AUTHORIZED;
    }

    /** Anything that is neither success nor pending is a final failure of that attempt. */
    public static boolean isFailure(int code) {
        return !isSuccess(code) && !isPending(code);
    }

    /**
     * Codes 10–47 reject the request itself (maintenance, auth, format, duplicates…). They say nothing about whether
     * the customer paid, so they must never be treated as a payment outcome.
     */
    public static boolean isRequestRejection(int code) {
        return code >= 10 && code <= 47;
    }
}
