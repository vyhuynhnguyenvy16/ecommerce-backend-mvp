package com.example.demo.payment;

import java.math.BigDecimal;

/** MoMo takes whole VND between 1.000 and 50.000.000 (captureWallet limits in the MoMo docs). */
public final class MomoAmount {

    public static final long MIN_VND = 1_000L;
    public static final long MAX_VND = 50_000_000L;

    private MomoAmount() {
    }

    public static long toVnd(BigDecimal total) {
        BigDecimal normalized = total.stripTrailingZeros();
        if (normalized.scale() > 0) {
            throw new PaymentNotAllowedException("MoMo chỉ nhận số tiền nguyên VND");
        }
        long vnd = normalized.longValueExact();
        if (vnd < MIN_VND || vnd > MAX_VND) {
            throw new PaymentNotAllowedException(
                    "MoMo chỉ nhận đơn từ 1.000đ đến 50.000.000đ");
        }
        return vnd;
    }
}
