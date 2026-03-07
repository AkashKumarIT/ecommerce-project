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
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = "inventory-events",
            groupId = "order-service"
    )
    @Transactional
    public void onInventoryEvents(Object payload) {
        try {
            if (payload instanceof InventoryReservedEvent reservedEvent) {
                handleInventoryReserved(reservedEvent);
                return;
            }

            if (payload instanceof InventoryReservationFailedEvent failedEvent) {
                handleInventoryFailed(failedEvent);
                return;
            }

            String json = payload instanceof String s
                    ? s
                    : objectMapper.writeValueAsString(payload);
            String eventType = objectMapper.readTree(json).path("eventType").asText("");

            switch (eventType) {
                case "INVENTORY_RESERVED" -> handleInventoryReserved(
                        objectMapper.readValue(json, InventoryReservedEvent.class)
                );
                case "INVENTORY_RESERVATION_FAILED" -> handleInventoryFailed(
                        objectMapper.readValue(json, InventoryReservationFailedEvent.class)
                );
                default -> log.debug("Ignoring inventory-events eventType={}", eventType);
            }
        } catch (Exception ex) {
            throw new IllegalArgumentException("Failed to process inventory-events payload", ex);
        }
    }

    private void handleInventoryReserved(InventoryReservedEvent event) {

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

        saveOutboxEvent(order, "ORDER_CONFIRMED", confirmedEvent);

        log.info("Order CONFIRMED: {}", event.getOrderId());
    }

    private void handleInventoryFailed(InventoryReservationFailedEvent event) {

        Order order = orderRepository
                .findByOrderNumber(event.getOrderId())
                .orElseThrow();

        if (order.getStatus() == OrderStatus.REJECTED) {
            log.info("Order already rejected. Ignoring duplicate event.");
            return;
        }

        order.setStatus(OrderStatus.REJECTED);
        orderRepository.save(order);

        OrderCancelledEvent cancelledEvent = new OrderCancelledEvent(
                order.getOrderNumber(),
                order.getCartId(),
                event.getReason()
        );

        saveOutboxEvent(order, "ORDER_CANCELLED", cancelledEvent);

        log.warn(
                "Order REJECTED: {} reason={}",
                event.getOrderId(),
                event.getReason()
        );
    }

    private void saveOutboxEvent(Order order, String eventType, Object eventPayload) {
        try {
            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .id(UUID.randomUUID())
                    .aggregateId(order.getId())
                    .aggregateType("ORDER")
                    .eventType(eventType)
                    .payload(objectMapper.writeValueAsString(eventPayload))
                    .status("NEW")
                    .createdAt(Instant.now())
                    .build();

            outboxEventRepository.save(outboxEvent);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to create order outbox event", e);
        }
    }
}