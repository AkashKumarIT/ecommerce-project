package com.ecom.inventory_service.kafka;

import com.ecom.events.inventory.InventoryReservationFailedEvent;
import com.ecom.events.inventory.InventoryReservedEvent;
import com.ecom.events.order.OrderCancelledEvent;
import com.ecom.events.order.OrderCreatedEvent;
import com.ecom.inventory_service.dto.InventoryReservationRequest;
import com.ecom.inventory_service.mapper.InventoryMapper;
import com.ecom.inventory_service.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class OrderEventConsumer {
    private final EventPublisher kafkaPublisher;
    private final InventoryService inventoryService;
    private final InventoryMapper mapper;
    @KafkaListener(topics = "order-events", groupId = "inventory-service")
    @Transactional
    public void handleOrderCreated(OrderCreatedEvent event) {

        try {
            InventoryReservationRequest request =
                    mapper.mapToReservation(event);

            inventoryService.reserveInventory(request);

            kafkaPublisher.publish(
                    "inventory-events",
                    event.getOrderId(),
                    new InventoryReservedEvent(event.getOrderId())
            );

        } catch (Exception ex) {

            kafkaPublisher.publish(
                    "inventory-events",
                    event.getOrderId(),
                    new InventoryReservationFailedEvent(
                            event.getOrderId(),
                            ex.getMessage()
                    )
            );
        }
    }

    @KafkaListener(topics = "order-events", groupId = "inventory-service")
    @Transactional
    public void handleOrderCancelled(OrderCancelledEvent event) {
        inventoryService.releaseInventory(event.getOrderId());
    }
}
