package com.ecom.paymentservice.kafka;

import com.ecom.events.order.OrderCancelledEvent;
import com.ecom.events.order.OrderCreatedEvent;
import com.ecom.paymentservice.service.PaymentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventConsumer {

    private final PaymentService paymentService;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = KafkaTopics.ORDER_EVENTS,
            groupId = "payment-service",
            containerFactory = "orderCreatedKafkaListenerContainerFactory"
    )
    public void handleOrderCreated(OrderCreatedEvent event) {

        log.info(
                "Received ORDER_CREATED event orderId={} cartId={}",
                event.getOrderId(),
                event.getCartId()
        );

        paymentService.createPaymentForOrder(event);
    }

    @KafkaListener(
            topics = KafkaTopics.ORDER_EVENTS,
            groupId = "payment-service",
            containerFactory = "orderCancelledKafkaListenerContainerFactory"
    )
    public void handleOrderCanceled(OrderCancelledEvent event) {

        log.info(
                "Received ORDER_CANCELLED event orderId={} cartId={}",
                event.getOrderId(),
                event.getCartId()
        );

        paymentService.cancelPaymentForOrder(event);
    }
}
