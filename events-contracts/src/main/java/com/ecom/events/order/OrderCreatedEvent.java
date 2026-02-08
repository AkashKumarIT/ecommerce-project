package com.ecom.events.order;

import com.ecom.events.base.DomainEvent;
import java.util.List;

public class OrderCreatedEvent extends DomainEvent {

    private String orderId;
    private List<OrderItem> items;

    public OrderCreatedEvent() {}

    public OrderCreatedEvent(String orderId, List<OrderItem> items) {
        super("ORDER_CREATED", 1, null, null);
        this.orderId = orderId;
        this.items = items;
    }

    public String getOrderId() {
        return orderId;
    }

    public List<OrderItem> getItems() {
        return items;
    }

    public static class OrderItem {
        private String sku;
        private Integer qty;

        public OrderItem() {}

        public OrderItem(String sku, Integer qty) {
            this.sku = sku;
            this.qty = qty;
        }

        public String getSku() {
            return sku;
        }

        public Integer getQty() {
            return qty;
        }
    }
}
