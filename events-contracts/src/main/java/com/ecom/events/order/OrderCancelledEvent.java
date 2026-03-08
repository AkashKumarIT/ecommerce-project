package com.ecom.events.order;

import com.ecom.events.base.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public class OrderCancelledEvent extends DomainEvent {

    private String orderId;
    private UUID cartId;
    private String reason;

    public OrderCancelledEvent() {}

    public OrderCancelledEvent(
            String orderId,
            UUID cartId,
            String reason
    ) {
        super(
                "ORDER_CANCELLED"
        );
        this.orderId = orderId;
        this.cartId = cartId;
        this.reason = reason;
    }

    public UUID getCartId() {
        return cartId;
    }

    public String getReason() {
        return reason;
    }

    public String getOrderId() {
        return orderId;
    }
}
