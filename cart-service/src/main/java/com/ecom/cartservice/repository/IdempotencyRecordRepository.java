package com.ecom.cartservice.repository;

import com.ecom.cartservice.model.IdempotencyRecord;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface IdempotencyRecordRepository extends JpaRepository<IdempotencyRecord, UUID> {
    Optional<IdempotencyRecord> findByIdempotencyKey(String key);

    @Modifying
    @Transactional
    @Query("DELETE FROM IdempotencyRecord i WHERE i.createdAt < :expiryTime")
    int deleteOlderThan(Instant expiryTime);
}
