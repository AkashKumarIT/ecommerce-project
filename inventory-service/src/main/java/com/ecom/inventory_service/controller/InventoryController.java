package com.ecom.inventory_service.controller;

import com.ecom.inventory_service.dto.InventoryReservationRequest;
import com.ecom.inventory_service.dto.InventoryResponse;
import com.ecom.inventory_service.model.InventoryStock;
import com.ecom.inventory_service.service.InventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    // ---------------- READ ----------------

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{sku}")
    public ResponseEntity<InventoryResponse> getInventory(@PathVariable String sku) {
        InventoryResponse stock = inventoryService.getInventory(sku);
        return ResponseEntity.ok(stock);
    }

    // ---------------- RESERVE ----------------

    @PostMapping("/reserve")
    public ResponseEntity<Void> reserveInventory(
            @Valid @RequestBody InventoryReservationRequest request) {

        inventoryService.reserveInventory(request);
        return ResponseEntity.ok().build();
    }

    // ---------------- RELEASE ----------------

    @PostMapping("/release/{orderId}")
    public ResponseEntity<Void> releaseInventory(@PathVariable String orderId) {
        inventoryService.releaseInventory(orderId);
        return ResponseEntity.ok().build();
    }

    // ---------------- CHECK STOCK ----------------

//    @PreAuthorize("isAuthenticated()")
    @GetMapping
    public ResponseEntity<List<InventoryResponse>> isInStock(@RequestParam List<String> sku) {
        return ResponseEntity.ok(inventoryService.isInStock(sku));
    }
}
