package com.ecom.cartservice.kafka;

import com.ecom.cartservice.service.CartService;
import com.ecom.events.order.OrderCancelledEvent;
import com.ecom.events.order.OrderConfirmedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CartOrderEventConsumer {

    private final CartService cartService;

    @KafkaListener(topics = "order-events", groupId = "cart-service")
    public void handleOrderConfirmed(OrderConfirmedEvent event) {
        cartService.handleOrderConfirmed(event);
    }

    @KafkaListener(topics = "order-events", groupId = "cart-service")
    public void handleOrderCancelled(OrderCancelledEvent event) {
        cartService.handleOrderCancelled(event);
    }
}
