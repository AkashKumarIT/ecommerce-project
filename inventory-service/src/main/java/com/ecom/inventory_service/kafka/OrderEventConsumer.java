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
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class OrderEventConsumer {
    private final EventPublisher kafkaPublisher;
    private final InventoryService inventoryService;
    private final InventoryMapper mapper;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "order-events", groupId = "inventory-service")
    @Transactional
    public void handleOrderCreated(OrderCreatedEvent event) {

        try {
            InventoryReservationRequest request =
                    mapper.mapToReservation(event);

            inventoryService.reserveInventory(request);

            InventoryReservedEvent reservedEvent =
                    new InventoryReservedEvent(event.getOrderId());

            OutboxEvent outbox = OutboxEvent.builder()
                    .id(UUID.randomUUID())
                    .aggregateId(UUID.fromString(event.getOrderId()))
                    .aggregateType("INVENTORY")
                    .eventType("INVENTORY_RESERVED")
                    .payload(objectMapper.writeValueAsString(reservedEvent))
                    .status("NEW")
                    .createdAt(Instant.now())
                    .build();

            outboxEventRepository.save(outbox);

        } catch (Exception ex) {

            InventoryReservationFailedEvent failedEvent =
                    new InventoryReservationFailedEvent(
                            event.getOrderId(),
                            ex.getMessage()
                    );

            String payload;

            try {
                payload = objectMapper.writeValueAsString(failedEvent);
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Failed to serialize event", e);
            }

            OutboxEvent outbox = OutboxEvent.builder()
                    .id(UUID.randomUUID())
                    .aggregateId(UUID.fromString(event.getOrderId()))
                    .aggregateType("INVENTORY")
                    .eventType("INVENTORY_RESERVATION_FAILED")
                    .payload(payload)
                    .status("NEW")
                    .createdAt(Instant.now())
                    .build();

            outboxEventRepository.save(outbox);
        }
    }

    @KafkaListener(topics = "order-events", groupId = "inventory-service")
    @Transactional
    public void handleOrderCancelled(OrderCancelledEvent event) {
        inventoryService.releaseInventory(event.getOrderId());
    }
}
