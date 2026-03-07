package com.ecom.orderservice.kafka;

import com.ecom.events.cart.CartCheckoutInitiatedEvent;
import com.ecom.orderservice.service.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CartEventConsumer {

    private final OrderService orderService;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = "cart-events",
            groupId = "order-service"
    )
    public void handleCartCheckout(Object payload) {
        try {
            CartCheckoutInitiatedEvent event;
            if (payload instanceof CartCheckoutInitiatedEvent typedEvent) {
                event = typedEvent;
            } else if (payload instanceof String json) {
                event = objectMapper.readValue(json, CartCheckoutInitiatedEvent.class);
            } else {
                event = objectMapper.convertValue(payload, CartCheckoutInitiatedEvent.class);
            }

            log.info("Received CART_CHECKOUT_INITIATED for cartId={}", event.getCartId());
            orderService.createOrderFromCartEvent(event);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Failed to process cart-events payload", ex);
        }
    }
}