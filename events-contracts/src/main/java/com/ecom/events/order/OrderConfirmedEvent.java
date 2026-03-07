package com.ecom.events.order;

import com.ecom.events.base.DomainEvent;
import java.time.Instant;
import java.util.UUID;

public class OrderConfirmedEvent extends DomainEvent {

    private String orderId;
    private UUID cartId;

    public OrderConfirmedEvent() {
        super();
    }

    public OrderConfirmedEvent(String orderId, UUID cartId) {
        super(
                "ORDER_CONFIRMED",
                1,
                UUID.randomUUID(),
                Instant.now()
        );
        this.orderId = orderId;
        this.cartId = cartId;
    }

    public String getOrderId() {
        return orderId;
    }

    public UUID getCartId() {
        return cartId;
    }
}