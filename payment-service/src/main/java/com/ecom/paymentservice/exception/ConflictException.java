package com.ecom.paymentservice.exception;

public class ConflictException extends BaseException {
    public ConflictException(String message) {
        super("CONFLICT", message);
    }
}
