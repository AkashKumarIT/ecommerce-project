package com.ecom.inventory_service.kafka;

import com.ecom.events.inventory.InventoryReservationFailedEvent;
import com.ecom.events.inventory.InventoryReservedEvent;
import com.ecom.events.order.OrderCancelledEvent;
import com.ecom.events.order.OrderCreatedEvent;
import com.ecom.inventory_service.dto.InventoryReservationRequest;
import com.ecom.inventory_service.mapper.InventoryMapper;
import com.ecom.inventory_service.model.OutboxEvent;
import com.ecom.inventory_service.repository.OutboxEventRepository;
import com.ecom.inventory_service.service.InventoryService;
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
public class OrderEventConsumer {

    private final InventoryService inventoryService;
    private final InventoryMapper mapper;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "order-events", groupId = "inventory-service")
    @Transactional
    public void handleOrderEvents(Object payload) {
        try {
            if (payload instanceof OrderCreatedEvent createdEvent) {
                handleOrderCreated(createdEvent);
                return;
            }

            if (payload instanceof OrderCancelledEvent cancelledEvent) {
                handleOrderCancelled(cancelledEvent);
                return;
            }

            String json = payload instanceof String s
                    ? s
                    : objectMapper.writeValueAsString(payload);
            String eventType = objectMapper.readTree(json).path("eventType").asText("");

            switch (eventType) {
                case "ORDER_CREATED" -> handleOrderCreated(
                        objectMapper.readValue(json, OrderCreatedEvent.class)
                );
                case "ORDER_CANCELLED" -> handleOrderCancelled(
                        objectMapper.readValue(json, OrderCancelledEvent.class)
                );
                default -> log.debug("Ignoring order-events eventType={}", eventType);
            }
        } catch (Exception ex) {
            throw new IllegalArgumentException("Failed to process order-events payload", ex);
        }
    }

    private void handleOrderCreated(OrderCreatedEvent event) {
        try {
            InventoryReservationRequest request = mapper.mapToReservation(event);
            if (request.getItems() == null || request.getItems().isEmpty()) {
                throw new IllegalStateException("ORDER_CREATED event has no items to reserve");
            }

            inventoryService.reserveInventory(request);

            InventoryReservedEvent reservedEvent =
                    new InventoryReservedEvent(event.getOrderId());

            saveOutbox(event.getOrderId(), "INVENTORY_RESERVED", reservedEvent);

        } catch (Exception ex) {
            InventoryReservationFailedEvent failedEvent =
                    new InventoryReservationFailedEvent(
                            event.getOrderId(),
                            ex.getMessage()
                    );

            saveOutbox(event.getOrderId(), "INVENTORY_RESERVATION_FAILED", failedEvent);
        }
    }

    private void handleOrderCancelled(OrderCancelledEvent event) {
        inventoryService.releaseInventory(event.getOrderId());
    }

    private void saveOutbox(String orderId, String eventType, Object event) {
        String payload;

        try {
            payload = objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize inventory outbox payload", e);
        }

        OutboxEvent outbox = OutboxEvent.builder()
                .id(UUID.randomUUID())
                .aggregateId(UUID.fromString(orderId))
                .aggregateType("INVENTORY")
                .eventType(eventType)
                .payload(payload)
                .status("NEW")
                .createdAt(Instant.now())
                .build();

        outboxEventRepository.save(outbox);
    }
}