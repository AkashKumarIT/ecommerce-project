package com.ecom.events.order;

import com.ecom.events.base.DomainEvent;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public class OrderCreatedEvent extends DomainEvent {

    private String orderId;
    private List<OrderItem> items;
    private UUID cartId;
    private String userId;
    private BigDecimal totalAmount;
    private String currency;

    public OrderCreatedEvent() {}

    public OrderCreatedEvent(
            String orderId,
            UUID cartId,
            String userId,
            List<OrderItem> items
    ) {
        super("ORDER_CREATED");
        this.orderId = orderId;
        this.cartId = cartId;
        this.userId = userId;
        this.items = items;
    }

    public OrderCreatedEvent(
            String orderId,
            UUID cartId,
            String userId,
            BigDecimal totalAmount,
            String currency,
            List<OrderItem> items
    ) {
        super("ORDER_CREATED");
        this.orderId = orderId;
        this.cartId = cartId;
        this.userId = userId;
        this.totalAmount = totalAmount;
        this.currency = currency;
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

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public String getCurrency() {
        return currency;
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

        public Integer getQuantity() {
            return qty;
        }
    }
}
