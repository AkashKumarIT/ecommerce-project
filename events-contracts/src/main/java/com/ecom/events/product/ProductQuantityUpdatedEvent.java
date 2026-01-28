package com.ecom.events.product;

import com.ecom.events.base.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public class ProductQuantityUpdatedEvent extends DomainEvent {

    private final String sku;
    private final int newQuantity;

    public ProductQuantityUpdatedEvent(String sku, int newQuantity) {
        super(
                "PRODUCT_QUANTITY_UPDATED",
                1,
                UUID.randomUUID(),
                Instant.now()
        );
        this.sku = sku;
        this.newQuantity = newQuantity;
    }

    public String getSku() {
        return sku;
    }

    public int getNewQuantity() {
        return newQuantity;
    }
}
