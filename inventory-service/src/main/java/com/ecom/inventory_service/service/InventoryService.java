package com.ecom.inventory_service.service;

import com.ecom.inventory_service.dto.InventoryReservationRequest;
import com.ecom.inventory_service.dto.InventoryResponse;
import com.ecom.inventory_service.model.InventoryStock;

import java.util.List;

public interface InventoryService {
    InventoryResponse getInventory(String sku);

    void reserveInventory(InventoryReservationRequest request);

    void releaseInventory(String orderId);

    List<InventoryResponse> isInStock(List<String> skus);
}
