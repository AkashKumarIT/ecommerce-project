package com.ecom.cartservice.kafka;

import com.ecom.cartservice.service.CartService;
import com.ecom.events.order.OrderCancelledEvent;
import com.ecom.events.order.OrderConfirmedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CartOrderEventConsumer {

    private final CartService cartService;
    private final ObjectMapper objectMapper;

    // ✅ Parameter ko 'Object payload' se 'String message' kar diya
    @KafkaListener(topics = "order-events", groupId = "cart-service-final")
    public void handleOrderEvents(String message) {
        log.info("Cart Service received raw order message: {}", message);
        try {
            // ✅ Double Serialization (extra quotes aur slashes) hatane ka logic
            String cleanMessage = message;
            if (message.startsWith("\"") && message.endsWith("\"")) {
                cleanMessage = objectMapper.readValue(message, String.class);
            }

            // Ab clean JSON ko aaram se parse karein
            String eventType = objectMapper.readTree(cleanMessage).path("eventType").asText("");

            switch (eventType) {
                case "ORDER_CONFIRMED" -> {
                    log.info("Processing ORDER_CONFIRMED in Cart Service");
                    cartService.handleOrderConfirmed(
                            objectMapper.readValue(cleanMessage, OrderConfirmedEvent.class)
                    );
                }
                case "ORDER_CANCELLED" -> {
                    log.info("Processing ORDER_CANCELLED in Cart Service");
                    cartService.handleOrderCancelled(
                            objectMapper.readValue(cleanMessage, OrderCancelledEvent.class)
                    );
                }
                default -> log.debug("Ignoring unsupported order eventType={}", eventType);
            }
        } catch (Exception ex) {
            // ❌ Exception throw mat karna, bas log karna taaki consumer block na ho
            log.error("Failed to process order-events payload: {}", message, ex);
        }
    }
}