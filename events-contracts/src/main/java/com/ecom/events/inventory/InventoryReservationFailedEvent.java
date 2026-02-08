package com.ecom.events.inventory;

import com.ecom.events.base.DomainEvent;

public class InventoryReservationFailedEvent extends DomainEvent {

    private String orderId;
    private String reason;

    public InventoryReservationFailedEvent() {}

    public InventoryReservationFailedEvent(String orderId, String reason) {
        super("INVENTORY_RESERVATION_FAILED", 1, null, null);
        this.orderId = orderId;
        this.reason = reason;
    }

    public String getOrderId() {
        return orderId;
    }

    public String getReason() {
        return reason;
    }
}
