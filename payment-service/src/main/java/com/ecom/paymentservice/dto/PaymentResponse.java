package com.ecom.paymentservice.dto;

import com.ecom.paymentservice.entity.Payment;
import com.ecom.paymentservice.entity.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentResponse(
        UUID id,
        String orderId,
        UUID cartId,
        String userId,
        BigDecimal amount,
        String currency,
        PaymentStatus status,
        String paymentProvider,
        String providerPaymentId,
        Instant createdAt,
        Instant updatedAt
) {
    public static PaymentResponse from(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getOrderId(),
                payment.getCartId(),
                payment.getUserId(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getStatus(),
                payment.getPaymentProvider(),
                payment.getProviderPaymentId(),
                payment.getCreatedAt(),
                payment.getUpdatedAt()
        );
    }
}
