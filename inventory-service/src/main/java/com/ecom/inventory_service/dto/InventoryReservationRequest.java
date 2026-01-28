package com.ecom.inventory_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class InventoryReservationRequest {
    @NotBlank
    private String orderId;

    @NotEmpty
    private List<Item> items;

    @Getter
    @Setter
    public static class Item {

        @NotBlank
        private String sku;

        @NotNull
        private Integer qty;
    }
}
