package com.ecom.events.payment;

import com.ecom.events.base.DomainEvent;

import java.math.BigDecimal;
import java.util.UUID;

public class PaymentFailedEvent extends DomainEvent {

    private UUID paymentId;
    private String orderId;
    private UUID cartId;
    private String userId;
    private BigDecimal amount;
    private String currency;
    private String reason;

    public PaymentFailedEvent() {}

    public PaymentFailedEvent(
            UUID paymentId,
            String orderId,
            UUID cartId,
            String userId,
            BigDecimal amount,
            String currency,
            String reason
    ) {
        super("PAYMENT_FAILED");
        this.paymentId = paymentId;
        this.orderId = orderId;
        this.cartId = cartId;
        this.userId = userId;
        this.amount = amount;
        this.currency = currency;
        this.reason = reason;
    }

    public UUID getPaymentId() { return paymentId; }
    public String getOrderId() { return orderId; }
    public UUID getCartId() { return cartId; }
    public String getUserId() { return userId; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public String getReason() { return reason; }
}
