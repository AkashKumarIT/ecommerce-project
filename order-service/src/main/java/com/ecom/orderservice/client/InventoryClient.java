package com.ecom.orderservice.client;

import com.ecom.orderservice.dto.InventoryReservationRequest;import com.ecom.orderservice.dto.InventoryResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import java.util.List;

// "inventory-service" naam Eureka/Discovery server ke liye hota hai
// "url" abhi ke liye hardcoded hai (Local testing ke liye)
@FeignClient(name = "inventory-service", url = "http://localhost:8082")
public interface InventoryClient {

    @GetMapping("/api/inventory")
    List<InventoryResponse> checkStock(@RequestParam("sku") List<String> skus);

    @PostMapping("/api/inventory/reserve")
    void reserveInventory(@RequestBody InventoryReservationRequest request);
}