package com.ecom.inventory_service.service;

import com.ecom.inventory_service.dto.InventoryReservationRequest;
import com.ecom.inventory_service.dto.InventoryResponse;
import com.ecom.inventory_service.exception.InsufficientStockException;
import com.ecom.inventory_service.exception.ResourceNotFoundException;
import com.ecom.inventory_service.model.InventoryReservation;
import com.ecom.inventory_service.model.InventoryStock;
import com.ecom.inventory_service.model.ReservationStatus;
import com.ecom.inventory_service.repository.InventoryReservationRepository;
import com.ecom.inventory_service.repository.InventoryStockRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
//import lombok.var;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    private final InventoryStockRepository stockRepository;
    private final InventoryReservationRepository reservationRepository;

    @Override
    @Cacheable(value = "inventory", key = "#sku")
    public InventoryResponse getInventory(String sku) {
        InventoryStock stock = stockRepository.findById(sku)
                .orElseThrow(() ->
                        new ResourceNotFoundException("SKU not found: " + sku));

        return new InventoryResponse(
                stock.getSku(),
                stock.getAvailableQty(),
                stock.getReservedQty(),
                stock.getUpdatedAt()
        );
    }

    // ---------------- RESERVE ----------------

    @Override
    @Transactional
    @CacheEvict(value = "inventory", allEntries = true)
    public void reserveInventory(InventoryReservationRequest request) {

        for (InventoryReservationRequest.Item item : request.getItems()) {

            // Idempotency check
            reservationRepository
                    .findByOrderIdAndSku(request.getOrderId(), item.getSku())
                    .ifPresent(r -> {
                        if (r.getStatus() == ReservationStatus.RESERVED) {
                            return;
                        }
                    });

            InventoryStock stock = stockRepository
                    .findBySkuForUpdate(item.getSku())
                    .orElseThrow(() -> new ResourceNotFoundException("SKU not found: " + item.getSku()));

            if (stock.getAvailableQty() < item.getQty()) {
                throw new InsufficientStockException("Insufficient stock for SKU: " + item.getSku());
            }

            stock.setAvailableQty(stock.getAvailableQty() - item.getQty());
            stock.setReservedQty(stock.getReservedQty() + item.getQty());
            stock.setUpdatedAt(LocalDateTime.now());

            stockRepository.save(stock);

            InventoryReservation reservation = InventoryReservation.builder()
                    .id(UUID.randomUUID())
                    .orderId(request.getOrderId())
                    .sku(item.getSku())
                    .qty(item.getQty())
                    .status(ReservationStatus.RESERVED)
                    .createdAt(LocalDateTime.now())
                    .build();

            reservationRepository.save(reservation);
        }
    }

    // ---------------- RELEASE ----------------

    @Override
    @Transactional
    @CacheEvict(value = "inventory", allEntries = true)
    public void releaseInventory(String orderId) {

        var reservations = reservationRepository
                .findByOrderIdAndStatus(orderId, ReservationStatus.RESERVED);

        for (InventoryReservation reservation : reservations) {

            InventoryStock stock = stockRepository
                    .findBySkuForUpdate(reservation.getSku())
                    .orElseThrow(() -> new RuntimeException("SKU not found: " + reservation.getSku()));

            stock.setAvailableQty(stock.getAvailableQty() + reservation.getQty());
            stock.setReservedQty(stock.getReservedQty() - reservation.getQty());
            stock.setUpdatedAt(LocalDateTime.now());

            stockRepository.save(stock);

            reservation.setStatus(ReservationStatus.RELEASED);
            reservationRepository.save(reservation);
        }
    }
}
