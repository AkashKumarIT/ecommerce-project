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

    @KafkaListener(topics = "order-events", groupId = "cart-service")
    public void handleOrderEvents(Object payload) {
        if (payload instanceof OrderConfirmedEvent confirmedEvent) {
            cartService.handleOrderConfirmed(confirmedEvent);
            return;
        }

        if (payload instanceof OrderCancelledEvent cancelledEvent) {
            cartService.handleOrderCancelled(cancelledEvent);
            return;
        }

        if (payload instanceof String json) {
            routeJsonEvent(json);
            return;
        }

        routeJsonEvent(writeValue(payload));
    }

    private void routeJsonEvent(String json) {
        try {
            String eventType = objectMapper.readTree(json).path("eventType").asText("");
            switch (eventType) {
                case "ORDER_CONFIRMED" -> cartService.handleOrderConfirmed(
                        objectMapper.readValue(json, OrderConfirmedEvent.class)
                );
                case "ORDER_CANCELLED" -> cartService.handleOrderCancelled(
                        objectMapper.readValue(json, OrderCancelledEvent.class)
                );
                default -> log.debug("Ignoring unsupported order eventType={}", eventType);
            }
        } catch (Exception ex) {
            throw new IllegalArgumentException("Failed to process order-events payload", ex);
        }
    }

    private String writeValue(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Failed to convert payload to JSON", ex);
        }
    }
}