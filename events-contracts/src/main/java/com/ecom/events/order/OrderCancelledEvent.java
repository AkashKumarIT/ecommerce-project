package com.ecom.events.order;

import com.ecom.events.base.DomainEvent;

public class OrderCancelledEvent extends DomainEvent {

    private String orderId;

    public OrderCancelledEvent() {}

    public OrderCancelledEvent(String orderId) {
        super("ORDER_CANCELLED", 1, null, null);
        this.orderId = orderId;
    }

    public String getOrderId() {
        return orderId;
    }
}
