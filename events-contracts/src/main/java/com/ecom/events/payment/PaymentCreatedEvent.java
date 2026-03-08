package com.ecom.events.payment;

import com.ecom.events.base.DomainEvent;

import java.math.BigDecimal;
import java.util.UUID;

public class PaymentCreatedEvent extends DomainEvent {

    private UUID paymentId;
    private String orderId;
    private UUID cartId;
    private String userId;
    private BigDecimal amount;
    private String currency;

    public PaymentCreatedEvent() {}

    public PaymentCreatedEvent(
            UUID paymentId,
            String orderId,
            UUID cartId,
            String userId,
            BigDecimal amount,
            String currency
    ) {
        super("PAYMENT_CREATED");
        this.paymentId = paymentId;
        this.orderId = orderId;
        this.cartId = cartId;
        this.userId = userId;
        this.amount = amount;
        this.currency = currency;
    }

    public UUID getPaymentId() { return paymentId; }
    public String getOrderId() { return orderId; }
    public UUID getCartId() { return cartId; }
    public String getUserId() { return userId; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
}
