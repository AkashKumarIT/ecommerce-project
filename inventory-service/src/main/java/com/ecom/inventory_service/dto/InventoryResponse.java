package com.ecom.inventory_service.dto;

import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryResponse implements Serializable {
    private String sku;
    private Integer availableQty;
    private Integer reservedQty;
    private LocalDateTime updatedAt;
}
