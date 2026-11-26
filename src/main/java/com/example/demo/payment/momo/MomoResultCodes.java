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
}
