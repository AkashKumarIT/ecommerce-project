package com.ecom.orderservice.kafka;

import com.ecom.events.inventory.InventoryReservationFailedEvent;
import com.ecom.events.inventory.InventoryReservedEvent;
import com.ecom.events.order.OrderCancelledEvent;
import com.ecom.events.order.OrderConfirmedEvent;
import com.ecom.orderservice.model.Order;
import com.ecom.orderservice.model.OrderStatus;
import com.ecom.orderservice.model.OutboxEvent;
import com.ecom.orderservice.repository.OrderRepository;
import com.ecom.orderservice.repository.OutboxEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class InventoryEventConsumer {

    private final OrderRepository orderRepository;
    private final EventPublisher eventPublisher;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = "inventory-events",
            groupId = "order-service"
    )
    @Transactional
    public void onInventoryReserved(InventoryReservedEvent event) {

        Order order = orderRepository
                .findByOrderNumber(event.getOrderId())
                .orElseThrow();

        if (order.getStatus() == OrderStatus.CONFIRMED) {
            log.info("Order already confirmed. Ignoring duplicate event.");
            return;
        }

        order.setStatus(OrderStatus.CONFIRMED);
        orderRepository.save(order);

        OrderConfirmedEvent confirmedEvent =
                new OrderConfirmedEvent(
                        order.getOrderNumber(),
                        order.getCartId()
                );

        OutboxEvent outboxEvent = null;
        try {
            outboxEvent = OutboxEvent.builder()
                    .id(UUID.randomUUID())
                    .aggregateId(order.getId())
                    .aggregateType("ORDER")
                    .eventType("ORDER_CONFIRMED")
                    .payload(objectMapper.writeValueAsString(confirmedEvent))
                    .status("NEW")
                    .createdAt(Instant.now())
                    .build();
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e + " Failed in onInventoryReserved of InventoryEventConsumer class");
        }

        outboxEventRepository.save(outboxEvent);

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

        if (order.getStatus() == OrderStatus.REJECTED) {
            log.info("Order already rejected. Ignoring duplicate event.");
            return;
        }

        order.setStatus(OrderStatus.REJECTED);
        orderRepository.save(order);

        eventPublisher.publish(
                "order-events",
                order.getOrderNumber(),
                new OrderCancelledEvent(
                        order.getOrderNumber(),
                        order.getCartId(),
                        event.getReason()
                )
        );
        

        log.warn(
                "Order REJECTED: {} reason={}",
                event.getOrderId(),
                event.getReason()
        );
    }
}
