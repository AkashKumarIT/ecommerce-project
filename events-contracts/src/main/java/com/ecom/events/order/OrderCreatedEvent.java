package com.ecom.events.order;

import com.ecom.events.base.DomainEvent;
import java.util.List;
import java.util.UUID;

public class OrderCreatedEvent extends DomainEvent {

    private String orderId;
    private List<OrderItem> items;
    private UUID cartId;
    private String userId;

    public OrderCreatedEvent() {}

    public OrderCreatedEvent(
            String orderId,
            UUID cartId,
            String userId,
            List<OrderItem> items
    ) {
        super("ORDER_CREATED", 1, null, null);
        this.orderId = orderId;
        this.cartId = cartId;
        this.userId = userId;
        this.items = items;
    }

    public UUID getCartId() {
        return cartId;
    }

    public String getUserId() {
        return userId;
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
