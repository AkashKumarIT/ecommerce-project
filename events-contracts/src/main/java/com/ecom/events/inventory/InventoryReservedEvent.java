package com.ecom.events.inventory;

import com.ecom.events.base.DomainEvent;

public class InventoryReservedEvent extends DomainEvent {

    private String orderId;

    public InventoryReservedEvent() {}

    public InventoryReservedEvent(String orderId) {
        super("INVENTORY_RESERVED", 1, null, null);
        this.orderId = orderId;
    }

    public String getOrderId() {
        return orderId;
    }
}
