package com.ecom.events.product;

import com.ecom.events.base.DomainEvent;

import java.time.Instant;
import java.util.UUID;


public class ProductCreatedEvent extends DomainEvent {

    private String sku;
    private int initialQuantity;

    public ProductCreatedEvent(){
        super();
    }
    public ProductCreatedEvent(String sku, int initialQuantity) {
        super(
                "PRODUCT_CREATED",
                1,
                UUID.randomUUID(),
                Instant.now()
        );
        this.sku = sku;
        this.initialQuantity = initialQuantity;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public void setInitialQuantity(int initialQuantity) {
        this.initialQuantity = initialQuantity;
    }

    public String getSku() {
        return sku;
    }

    public int getInitialQuantity() {
        return initialQuantity;
    }
}
