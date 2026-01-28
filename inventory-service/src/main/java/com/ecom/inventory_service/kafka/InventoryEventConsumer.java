package com.ecom.inventory_service.kafka;

import com.ecom.events.product.ProductCreatedEvent;
import com.ecom.events.product.ProductQuantityUpdatedEvent;
import com.ecom.inventory_service.model.InventoryStock;
import com.ecom.inventory_service.repository.InventoryStockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class InventoryEventConsumer {

    private final InventoryStockRepository stockRepository;

    @KafkaListener(
            topics = "product-events",
            groupId = "inventory-service"
    )
    @Transactional
    public void consume(ConsumerRecord<String, Object> record) {

        Object event = record.value();

        log.info("consumed event: {}", event);
        log.info("Event Class Type: {}", event.getClass().getName());

        if (event instanceof ProductCreatedEvent e) {
            log.info("It is a ProductCreatedEvent! Processing...");
            handleProductCreated(e);
        }else {
            // 2. THIS WILL TELL US IF IT'S FAILING THE CHECK
            log.warn("Event is NOT a ProductCreatedEvent. It is: {}", event.getClass().getName());
        }

        if (event instanceof ProductQuantityUpdatedEvent e) {
            handleProductQuantityUpdated(e);
        }
    }

    private void handleProductCreated(ProductCreatedEvent event) {

        log.info("🔥 Received event from Kafka: {}", event);

        // 🔁 Idempotency check
        if (stockRepository.existsBySku(event.getSku())) {
            return;
        }

        InventoryStock stock = InventoryStock.builder()
                .sku(event.getSku())
                .availableQty(event.getInitialQuantity())
                .reservedQty(0)
                .updatedAt(LocalDateTime.now())
                .build();

        stockRepository.save(stock);

    }

    private void handleProductQuantityUpdated(ProductQuantityUpdatedEvent event) {

        InventoryStock stock = stockRepository.findById(event.getSku())
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Inventory not found for SKU: " + event.getSku()
                        ));

        stock.setAvailableQty(event.getNewQuantity());
        stock.setUpdatedAt(LocalDateTime.now());

        stockRepository.save(stock);
    }
}
