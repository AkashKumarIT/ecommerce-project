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
    // ✅ Parameter ko Object se String mein change kar diya
    public void handleCartCheckout(String message) {
        log.info("Order Service received raw cart message: {}", message);
        try {
            // ✅ Double Serialization (extra quotes aur slashes) hatane ka logic
            String cleanMessage = message;
            if (message.startsWith("\"") && message.endsWith("\"")) {
                cleanMessage = objectMapper.readValue(message, String.class);
            }

            // Ab clean string ko directly apne Event object mein map karein
            CartCheckoutInitiatedEvent event = objectMapper.readValue(cleanMessage, CartCheckoutInitiatedEvent.class);

            log.info("Successfully parsed CART_CHECKOUT_INITIATED for cartId={}", event.getCartId());

            // Order create karne wala method call karein
            orderService.createOrderFromCartEvent(event);

        } catch (Exception ex) {
            // ❌ Exception throw mat karna, warna loop mein fass jayega. Sirf log error karo.
            log.error("Failed to process cart-events payload: {}", message, ex);
        }
    }
}