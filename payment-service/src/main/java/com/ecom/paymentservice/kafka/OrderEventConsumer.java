package com.ecom.paymentservice.kafka;

import com.ecom.events.order.OrderCancelledEvent;
import com.ecom.events.order.OrderCreatedEvent;
import com.ecom.paymentservice.service.PaymentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;


//@Component
//@RequiredArgsConstructor
//@Slf4j
//// ✅ Listener ab class level par aayega
//@KafkaListener(topics = KafkaTopics.ORDER_EVENTS, groupId = "payment-service")
//public class OrderEventConsumer {
//
//    private final PaymentService paymentService;
//
//    // ✅ Spring Boot apne aap pehchan lega ki ye Created event hai
//    @KafkaHandler
//    public void handleOrderCreated(OrderCreatedEvent event) {
//        log.info("Processing ORDER_CREATED for Order ID: {}", event.getOrderId());
//        paymentService.createPaymentForOrder(event);
//    }
//
//    // ✅ Agar Cancelled event aata hai, toh ye method chalega
//    @KafkaHandler
//    public void handleOrderCancelled(OrderCancelledEvent event) {
//        log.info("Processing ORDER_CANCELLED for Order ID: {}", event.getOrderId());
//        paymentService.cancelPaymentForOrder(event);
//    }
//
//    // ✅ Agar koi aisi chiz aa jaye jo system ko na pata ho
//    @KafkaHandler(isDefault = true)
//    public void handleUnknown(Object object) {
//        log.warn("Received unknown message type: {}", object.getClass());
//    }
//}


@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventConsumer {

    private final PaymentService paymentService;
    private final ObjectMapper objectMapper;

    // ✅ Naya groupId aur Parameter ab String hai
    @KafkaListener(topics = "order-events", groupId = "payment-service-final")
    public void handleOrderEvents(String message) {
        log.info("Payment Service received raw order message: {}", message);
        try {
            // ✅ Double Serialization fix (extra quotes hatane ke liye)
            String cleanMessage = message;
            if (message.startsWith("\"") && message.endsWith("\"")) {
                cleanMessage = objectMapper.readValue(message, String.class);
            }

            // ✅ JSON node se asli eventType nikalna
            String eventType = objectMapper.readTree(cleanMessage).path("eventType").asText("");

            if ("ORDER_CREATED".equals(eventType)) {
                OrderCreatedEvent event = objectMapper.readValue(cleanMessage, OrderCreatedEvent.class);
                log.info("Processing ORDER_CREATED for Order ID: {}", event.getOrderId());
                paymentService.createPaymentForOrder(event);

            } else if ("ORDER_CANCELLED".equals(eventType)) {
                OrderCancelledEvent event = objectMapper.readValue(cleanMessage, OrderCancelledEvent.class);
                log.info("Processing ORDER_CANCELLED for Order ID: {}", event.getOrderId());
                // ✅ Ab ye correct method call karega aur status REFUNDED/CANCELLED hoga!
                paymentService.cancelPaymentForOrder(event);

            } else {
                log.debug("Ignored event type in Payment Service: {}", eventType);
            }
        } catch (Exception e) {
            log.error("Failed to parse order-event payload: {}", message, e);
        }
    }
}
