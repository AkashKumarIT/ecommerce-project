package com.ecom.orderservice.kafka;

import com.ecom.events.payment.PaymentCompletedEvent;
import com.ecom.events.payment.PaymentFailedEvent;
import com.ecom.orderservice.model.OrderStatus;
import com.ecom.orderservice.service.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventConsumer {

    private final OrderService orderService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "payment-events", groupId = "order-service")
    public void handlePaymentEvents(String message) {
        log.info("Order Service received raw payment message: {}", message);
        try {
            // ✅ Double Serialization Fix: Agar string mein extra quotes hain, to unhe htayenge
            String unescapedMessage = message;
            if (message.startsWith("\"") && message.endsWith("\"")) {
                unescapedMessage = objectMapper.readValue(message, String.class);
            }

            // Ab hum saaf JSON se type padhenge
            String eventType = objectMapper.readTree(unescapedMessage).path("eventType").asText("");

            if ("PAYMENT_COMPLETED".equals(eventType)) {
                PaymentCompletedEvent event = objectMapper.readValue(unescapedMessage, PaymentCompletedEvent.class);
                log.info("Processing PAYMENT_COMPLETED for Order ID: {}", event.getOrderId());
                orderService.updateOrderStatus(event.getOrderId(), OrderStatus.PAYMENT_CONFIRMED);

            } else if ("PAYMENT_FAILED".equals(eventType)) {
                PaymentFailedEvent event = objectMapper.readValue(unescapedMessage, PaymentFailedEvent.class);
                log.info("Processing PAYMENT_FAILED for Order ID: {}", event.getOrderId());
                orderService.updateOrderStatus(event.getOrderId(), OrderStatus.CANCELLED);

            } else {
                log.debug("Ignored event type: {}", eventType);
            }
        } catch (Exception e) {
            log.error("Failed to parse payment-event payload: {}", message, e);
        }
    }
}