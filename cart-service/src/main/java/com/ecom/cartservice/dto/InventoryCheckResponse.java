package com.ecom.cartservice.dto;

import lombok.Data;

@Data
public class InventoryCheckResponse {

    private boolean available;
    private Integer availableQuantity;
}

