package com.ecom.events.cart;

import com.ecom.events.base.DomainEvent;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;


public class CartCheckoutInitiatedEvent extends DomainEvent {

    private UUID cartId;
    private String userId;
    private BigDecimal totalAmount;
    private List<Item> items;

    public CartCheckoutInitiatedEvent() {
        super();
    }

    public CartCheckoutInitiatedEvent(
            UUID cartId,
            String userId,
            BigDecimal totalAmount,
            List<Item> items
    ) {
        super(
                "CART_CHECKOUT_INITIATED",
                1,
                UUID.randomUUID(),
                Instant.now()
        );

        this.cartId = cartId;
        this.userId = userId;
        this.totalAmount = totalAmount;
        this.items = items;
    }

    public UUID getCartId() {
        return cartId;
    }

    public String getUserId() {
        return userId;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public List<Item> getItems() {
        return items;
    }

    public static class Item {

        private String sku;
        private Integer quantity;
        private BigDecimal price;

        public Item() {}

        public Item(String sku, Integer quantity, BigDecimal price) {
            this.sku = sku;
            this.quantity = quantity;
            this.price = price;
        }

        public String getSku() {
            return sku;
        }

        public Integer getQuantity() {
            return quantity;
        }

        public BigDecimal getPrice() {
            return price;
        }
    }
}
