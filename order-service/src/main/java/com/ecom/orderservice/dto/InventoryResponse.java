package com.ecom.orderservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class InventoryResponse {

    private String sku;
    private Integer availableQty;
}