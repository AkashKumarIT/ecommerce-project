package com.ecom.paymentservice.repository;

import com.ecom.paymentservice.entity.Payment;
import com.ecom.paymentservice.entity.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    Optional<Payment> findByOrderId(String orderId);
    boolean existsByOrderId(String orderId);
    List<Payment> findByStatusInAndCreatedAtBefore(
            List<PaymentStatus> statuses,
            Instant time
    );
}
