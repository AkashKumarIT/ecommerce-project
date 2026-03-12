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
    // ✅ Parameter 'Object payload' ki jagah 'String message' kar diya
    public void onInventoryEvents(String message) {
        log.info("Order Service received raw inventory message: {}", message);
        try {
            // ✅ Double Serialization (extra quotes) hatane ka logic
            String cleanMessage = message;
            if (message.startsWith("\"") && message.endsWith("\"")) {
                cleanMessage = objectMapper.readValue(message, String.class);
            }

            // ✅ Clean JSON se eventType nikalna
            String eventType = objectMapper.readTree(cleanMessage).path("eventType").asText("");

            switch (eventType) {
                case "INVENTORY_RESERVED" -> handleInventoryReserved(
                        objectMapper.readValue(cleanMessage, InventoryReservedEvent.class)
                );
                case "INVENTORY_RESERVATION_FAILED" -> handleInventoryFailed(
                        objectMapper.readValue(cleanMessage, InventoryReservationFailedEvent.class)
                );
                default -> log.debug("Ignoring inventory-events eventType={}", eventType);
            }
        } catch (Exception ex) {
            // ❌ Error throw karne ki jagah log karein taaki consumer atke nahi
            log.error("Failed to process inventory-events payload: {}", message, ex);
        }
    }

    private void handleInventoryReserved(InventoryReservedEvent event) {

        Order order = orderRepository
                .findByOrderNumber(event.getOrderId())
                .orElseThrow(() -> new com.ecom.orderservice.exception.ResourceNotFoundException("Order not found"));

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
                .orElseThrow(() -> new com.ecom.orderservice.exception.ResourceNotFoundException("Order not found"));

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
