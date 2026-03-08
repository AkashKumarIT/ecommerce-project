package com.ecom.orderservice.kafka;

import com.ecom.events.order.OrderCancelledEvent;
import com.ecom.events.payment.PaymentCompletedEvent;
import com.ecom.events.payment.PaymentFailedEvent;
import com.ecom.orderservice.model.Order;
import com.ecom.orderservice.model.OrderStatus;
import com.ecom.orderservice.model.OutboxEvent;
import com.ecom.orderservice.repository.OrderRepository;
import com.ecom.orderservice.repository.OutboxEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventConsumer {

    private final OrderRepository orderRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "payment-events", groupId = "order-service")
    @Transactional
    public void onPaymentEvents(Object payload) {
        try {
            if (payload instanceof PaymentCompletedEvent completedEvent) {
                handlePaymentCompleted(completedEvent);
                return;
            }

            if (payload instanceof PaymentFailedEvent failedEvent) {
                handlePaymentFailed(failedEvent);
                return;
            }

            String json = payload instanceof String s ? s : objectMapper.writeValueAsString(payload);
            String eventType = objectMapper.readTree(json).path("eventType").asText("");

            switch (eventType) {
                case "PAYMENT_COMPLETED" -> handlePaymentCompleted(objectMapper.readValue(json, PaymentCompletedEvent.class));
                case "PAYMENT_FAILED" -> handlePaymentFailed(objectMapper.readValue(json, PaymentFailedEvent.class));
                default -> log.debug("Ignoring payment-events eventType={}", eventType);
            }
        } catch (Exception ex) {
            throw new IllegalArgumentException("Failed to process payment-events payload", ex);
        }
    }

    private void handlePaymentCompleted(PaymentCompletedEvent event) {
        Order order = orderRepository.findByOrderNumber(event.getOrderId()).orElseThrow(() -> new com.ecom.orderservice.exception.ResourceNotFoundException("Order not found"));

        if (order.getStatus() == OrderStatus.PAYMENT_CONFIRMED || order.getStatus() == OrderStatus.CONFIRMED) {
            log.info("Order already marked payment confirmed. orderId={}", event.getOrderId());
            return;
        }

        order.setStatus(OrderStatus.PAYMENT_CONFIRMED);
        orderRepository.save(order);

        log.info("Payment confirmed for orderId={} paymentId={}", event.getOrderId(), event.getPaymentId());
    }

    private void handlePaymentFailed(PaymentFailedEvent event) {
        Order order = orderRepository.findByOrderNumber(event.getOrderId()).orElseThrow(() -> new com.ecom.orderservice.exception.ResourceNotFoundException("Order not found"));

        if (order.getStatus() == OrderStatus.CANCELLED || order.getStatus() == OrderStatus.REJECTED) {
            log.info("Order already terminal. Ignoring PAYMENT_FAILED orderId={}", event.getOrderId());
            return;
        }

        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);

        OrderCancelledEvent cancelledEvent = new OrderCancelledEvent(
                order.getOrderNumber(),
                order.getCartId(),
                event.getReason()
        );

        saveOutboxEvent(order, "ORDER_CANCELLED", cancelledEvent);
        log.warn("Order cancelled due to payment failure. orderId={} reason={}", event.getOrderId(), event.getReason());
    }

    private void saveOutboxEvent(Order order, String eventType, Object eventPayload) {
        try {
            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .id(UUID.randomUUID())
                    .aggregateId(order.getId())
                    .aggregateType("ORDER")
                    .eventType(eventType)
                    .payload(objectMapper.writeValueAsString(eventPayload))
                    .status("NEW")
                    .createdAt(Instant.now())
                    .build();

            outboxEventRepository.save(outboxEvent);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to create order outbox event", e);
        }
    }
}

