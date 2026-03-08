package com.ecom.paymentservice.provider;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Component
public class DummyPaymentProvider implements PaymentProvider {

    @Override
    public String providerName() {
        return "DUMMY";
    }

    @Override
    public String createPayment(String orderId, BigDecimal amount, String currency) {
        return "dummy_" + UUID.randomUUID();
    }

    @Override
    public boolean verifyPayment(String providerPaymentId) {
        return providerPaymentId != null && !providerPaymentId.isBlank();
    }
}
