package com.ecom.orderservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class InventoryReservationRequest {
    private String orderId;
    private List<Item> items;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class Item {
        private String sku; // Matching 'sku' from your Inventory Service DTO [cite: 19]
        private Integer qty;
    }
}