package com.ecom.inventory_service.kafka;

import com.ecom.events.inventory.InventoryReservationFailedEvent;
import com.ecom.events.inventory.InventoryReservedEvent;
import com.ecom.events.order.OrderCancelledEvent;
import com.ecom.events.payment.PaymentCompletedEvent;
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
// ✅ org.springframework.transaction.annotation.Transactional import hata diya gaya hai

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

    // ✅ YAHAN SE @Transactional HATA DIYA HAI TAARI OUTBOX EVENT SAVE HO SAKE
    @KafkaListener(topics = "payment-events", groupId = "inventory-service")
    public void handlePaymentEvents(String message) {
        log.info("Inventory Service received payment-event: {}", message);
        try {
            String cleanMessage = message;
            if (message.startsWith("\"") && message.endsWith("\"")) {
                cleanMessage = objectMapper.readValue(message, String.class);
            }

            String eventType = objectMapper.readTree(cleanMessage).path("eventType").asText("");

            if ("PAYMENT_COMPLETED".equals(eventType)) {
                PaymentCompletedEvent event = objectMapper.readValue(cleanMessage, PaymentCompletedEvent.class);
                handlePaymentCompleted(event);
            } else {
                log.debug("Ignoring eventType={} in inventory-service", eventType);
            }
        } catch (Exception ex) {
            log.error("Failed to process payment-events payload", ex);
        }
    }

    // ✅ YAHAN SE BHI @Transactional HATA DIYA HAI
    @KafkaListener(topics = "order-events", groupId = "inventory-service")
    public void handleOrderEvents(String message) {
        log.info("Inventory Service received order-event: {}", message);
        try {
            String cleanMessage = message;
            if (message.startsWith("\"") && message.endsWith("\"")) {
                cleanMessage = objectMapper.readValue(message, String.class);
            }

            String eventType = objectMapper.readTree(cleanMessage).path("eventType").asText("");

            if ("ORDER_CANCELLED".equals(eventType)) {
                OrderCancelledEvent event = objectMapper.readValue(cleanMessage, OrderCancelledEvent.class);
                handleOrderCancelled(event);
            }
        } catch (Exception ex) {
            log.error("Failed to process order-events payload", ex);
        }
    }

    private void handlePaymentCompleted(PaymentCompletedEvent event) {
        try {
            InventoryReservationRequest request = mapper.mapToReservation(event);

            if (request.getItems() == null || request.getItems().isEmpty()) {
                throw new IllegalStateException("PAYMENT_COMPLETED event has no items to reserve");
            }

            inventoryService.reserveInventory(request);
            InventoryReservedEvent reservedEvent = new InventoryReservedEvent(event.getOrderId());
            saveOutbox(event.getOrderId(), "INVENTORY_RESERVED", reservedEvent);

            log.info("Inventory reserved for orderId={} after successful payment", event.getOrderId());
        } catch (Exception ex) {
            // ✅ Kahaani ka Hero: Ab yeh naya event DB mein independently save hoga!
            InventoryReservationFailedEvent failedEvent = new InventoryReservationFailedEvent(
                    event.getOrderId(),
                    ex.getMessage()
            );

            saveOutbox(event.getOrderId(), "INVENTORY_RESERVATION_FAILED", failedEvent);
            log.error("Inventory reservation failed for orderId={} after payment. Sent Rollback Event.", event.getOrderId());
        }
    }

    private void handleOrderCancelled(OrderCancelledEvent event) {
        inventoryService.releaseInventory(event.getOrderId());
        log.info("Inventory release requested for cancelled orderId={}", event.getOrderId());
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