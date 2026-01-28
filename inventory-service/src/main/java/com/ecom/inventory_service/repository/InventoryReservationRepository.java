package com.ecom.inventory_service.repository;

import com.ecom.inventory_service.model.InventoryReservation;
import com.ecom.inventory_service.model.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InventoryReservationRepository extends JpaRepository<InventoryReservation, UUID> {
    Optional<InventoryReservation> findByOrderIdAndSku(String orderId, String sku);

    List<InventoryReservation> findByOrderIdAndStatus(String orderId, ReservationStatus status);
}
