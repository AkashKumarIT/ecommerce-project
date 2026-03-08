package com.ecom.paymentservice.provider;

import java.math.BigDecimal;

public interface PaymentProvider {
    String providerName();
    String createPayment(String orderId, BigDecimal amount, String currency);
    boolean verifyPayment(String providerPaymentId);
}
