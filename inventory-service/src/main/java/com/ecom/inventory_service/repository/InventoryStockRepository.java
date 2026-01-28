package com.ecom.inventory_service.repository;

import com.ecom.inventory_service.model.InventoryStock;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface InventoryStockRepository extends JpaRepository<InventoryStock,String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM InventoryStock i WHERE i.sku = :sku")
    Optional<InventoryStock> findBySkuForUpdate(@Param("sku") String sku);

    boolean existsBySku(String sku);
}
