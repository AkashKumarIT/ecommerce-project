package com.ecom.orderservice.repository;

import com.ecom.orderservice.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
     Optional<Order> findByOrderNumber(String orderNumber);

     boolean existsByCartId(UUID cartId);

     Optional<Order> findByCartId(UUID cartId);
}
