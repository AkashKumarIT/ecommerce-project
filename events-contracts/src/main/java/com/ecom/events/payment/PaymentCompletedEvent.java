package com.ecom.events.payment;

import com.ecom.events.base.DomainEvent;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public class PaymentCompletedEvent extends DomainEvent {

    private UUID paymentId;
    private String orderId;
    private UUID cartId;
    private String userId;
    private BigDecimal amount;
    private String currency;
    private List<Item> items;

    public PaymentCompletedEvent() {}

    public PaymentCompletedEvent(
            UUID paymentId,
            String orderId,
            UUID cartId,
            String userId,
            BigDecimal amount,
            String currency,
            List<Item> items
    ) {
        super("PAYMENT_COMPLETED");
        this.paymentId = paymentId;
        this.orderId = orderId;
        this.cartId = cartId;
        this.userId = userId;
        this.amount = amount;
        this.currency = currency;
        this.items = items;
    }

    public UUID getPaymentId() { return paymentId; }
    public String getOrderId() { return orderId; }
    public UUID getCartId() { return cartId; }
    public String getUserId() { return userId; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public List<Item> getItems() { return items; }

    public static class Item {
        private String sku;
        private Integer qty;

        public Item() {}

        public Item(String sku, Integer qty) {
            this.sku = sku;
            this.qty = qty;
        }

        public String getSku() { return sku; }
        public Integer getQty() { return qty; }
        public Integer getQuantity() { return qty; }
    }
}
