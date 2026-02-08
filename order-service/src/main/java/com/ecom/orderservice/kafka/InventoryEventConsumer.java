package com.ecom.orderservice.kafka;

import com.ecom.events.inventory.InventoryReservationFailedEvent;
import com.ecom.events.inventory.InventoryReservedEvent;
import com.ecom.orderservice.model.Order;
import com.ecom.orderservice.model.OrderStatus;
import com.ecom.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class InventoryEventConsumer {

    private final OrderRepository orderRepository;

    @KafkaListener(
            topics = "inventory-events",
            groupId = "order-service"
    )
    @Transactional
    public void onInventoryReserved(InventoryReservedEvent event) {

        Order order = orderRepository
                .findByOrderNumber(event.getOrderId())
                .orElseThrow();

        order.setStatus(OrderStatus.CONFIRMED);
        orderRepository.save(order);

        log.info("Order CONFIRMED: {}", event.getOrderId());
    }

    @KafkaListener(
            topics = "inventory-events",
            groupId = "order-service"
    )
    @Transactional
    public void onInventoryFailed(InventoryReservationFailedEvent event) {

        Order order = orderRepository
                .findByOrderNumber(event.getOrderId())
                .orElseThrow();

        order.setStatus(OrderStatus.REJECTED);
        orderRepository.save(order);

        log.warn(
                "Order REJECTED: {} reason={}",
                event.getOrderId(),
                event.getReason()
        );
    }
}
