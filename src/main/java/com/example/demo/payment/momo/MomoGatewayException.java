package com.example.demo.payment.momo;

/** MoMo rejected a request or could not be reached. */
public class MomoGatewayException extends RuntimeException {

    private final Integer resultCode;

    public MomoGatewayException(String message, Integer resultCode) {
        super(message);
        this.resultCode = resultCode;
    }

    public MomoGatewayException(String message, Throwable cause) {
        super(message, cause);
        this.resultCode = null;
    }

    public Integer getResultCode() {
        return resultCode;
    }
}
