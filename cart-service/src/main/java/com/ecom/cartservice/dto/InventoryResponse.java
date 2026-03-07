package com.ecom.cartservice.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class InventoryResponse {

    private String sku;
    private Integer availableQty;
    private Integer reservedQty;
    private LocalDateTime updatedAt;
}
