package com.ecom.orderservice.kafka;

import com.ecom.events.cart.CartCheckoutInitiatedEvent;
import com.ecom.orderservice.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CartEventConsumer {

    private final OrderService orderService;

    @KafkaListener(
            topics = "cart-events",
            groupId = "order-service"
    )
    public void handleCartCheckout(CartCheckoutInitiatedEvent event) {

        log.info("Received CART_CHECKOUT_INITIATED for cartId={}",
                event.getCartId());

        orderService.createOrderFromCartEvent(event);
    }
}
