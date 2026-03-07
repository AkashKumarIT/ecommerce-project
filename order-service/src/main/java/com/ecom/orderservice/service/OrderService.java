package com.ecom.orderservice.service;

import com.ecom.events.cart.CartCheckoutInitiatedEvent;
import com.ecom.events.order.OrderCancelledEvent;
import com.ecom.events.order.OrderConfirmedEvent;
import com.ecom.events.order.OrderCreatedEvent;
import com.ecom.orderservice.dto.OrderRequest;
import com.ecom.orderservice.kafka.EventPublisher;
import com.ecom.orderservice.mappers.Mapper;
import com.ecom.orderservice.model.Order;
import com.ecom.orderservice.model.OrderLineItems;
import com.ecom.orderservice.model.OrderStatus;
import com.ecom.orderservice.model.OutboxEvent;
import com.ecom.orderservice.repository.OrderRepository;
import com.ecom.orderservice.repository.OutboxEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ecom.orderservice.exception.BaseException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final EventPublisher eventPublisher;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;
    private final Mapper mapper;

    @Transactional
    public String placeOrder(OrderRequest request) {

        String orderNumber = UUID.randomUUID().toString();

        Order order = new Order();
        order.setOrderNumber(orderNumber);
        order.setStatus(OrderStatus.PENDING);

        List<OrderLineItems> items = request.getOrderLineItemsDtoList()
                .stream()
                .map(mapper::mapToDto)
                .toList();

        order.setOrderLineItemsList(items);
        orderRepository.save(order);

        // 🔥 Publish event
        OrderCreatedEvent event = new OrderCreatedEvent(
                orderNumber,
                null,
                null,
                items.stream()
                        .map(i -> new OrderCreatedEvent.OrderItem(
                                i.getSku(), i.getQuantity()))
                        .toList()
        );

//        eventPublisher.publish("order-events", orderNumber, event);
        OrderCreatedEvent createdEvent =
                new OrderCreatedEvent(
                        orderNumber,
                        order.getCartId(),
                        null,
                        null
                );

        OutboxEvent outboxEvent = null;
        try {
            outboxEvent = OutboxEvent.builder()
                    .id(UUID.randomUUID())
                    .aggregateId(order.getId())
                    .aggregateType("ORDER")
                    .eventType("ORDER_CREATED")
                    .payload(objectMapper.writeValueAsString(createdEvent))
                    .status("NEW")
                    .createdAt(Instant.now())
                    .build();
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e + "error in createdEvent outbox event creation inside orderSevice placeOrder");
        }

        outboxEventRepository.save(outboxEvent);
        return orderNumber;
    }

    public String cancelOrder(String orderNumber) {
        // 1. Find the order by order number
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderNumber));

        // 2. Call Inventory Service to RELEASE the reserved stock
        log.info("Calling Inventory Service to Release Stock for Order: {}", orderNumber);


        // 3. Update order status to CANCELLED
        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);

        OrderCancelledEvent cancelledEvent =
                new OrderCancelledEvent(
                        orderNumber,
                        order.getCartId(),
                        "MANUAL_CANCEL"
                );

        OutboxEvent outboxEvent = null;
        try {
            outboxEvent = OutboxEvent.builder()
                    .id(UUID.randomUUID())
                    .aggregateId(order.getId())
                    .aggregateType("ORDER")
                    .eventType("ORDER_CANCELLED")
                    .payload(objectMapper.writeValueAsString(cancelledEvent))
                    .status("NEW")
                    .createdAt(Instant.now())
                    .build();
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e + "error in cancelOrder outbox event creation inside orderSevice cancelOrder");
        }

        outboxEventRepository.save(outboxEvent);


        log.info("Order Cancelled Successfully: {}", orderNumber);
        return "Order Cancelled Successfully";
    }

    @Transactional
    public void createOrderFromCartEvent(
            CartCheckoutInitiatedEvent event
    ) {
        if (orderRepository.existsByCartId(event.getCartId())) {
            log.info("Order already exists for cart {}", event.getCartId());
            return;
        }

        String orderNumber = UUID.randomUUID().toString();

        Order order = new Order();
        order.setOrderNumber(orderNumber);
        order.setStatus(OrderStatus.PENDING);

        // ADD THIS LINE
        order.setCartId(event.getCartId());
        order.setUserId(event.getUserId());

        List<OrderLineItems> items =
                event.getItems().stream()
                        .map(i -> {
                            OrderLineItems item = new OrderLineItems();
                            item.setSku(i.getSku());
                            item.setQuantity(i.getQuantity());
                            item.setPrice(i.getPrice());
                            return item;
                        })
                        .toList();

        order.setOrderLineItemsList(items);

        orderRepository.save(order);

        // 🔥 Publish ORDER_CREATED (same as placeOrder)
        OrderCreatedEvent orderCreatedEvent =
                new OrderCreatedEvent(
                        orderNumber,
                        event.getCartId(),
                        event.getUserId(),
                        items.stream()
                                .map(i -> new OrderCreatedEvent.OrderItem(
                                        i.getSku(),
                                        i.getQuantity()))
                                .toList()
                );

        OutboxEvent outboxEvent = null;
        try {
            outboxEvent = OutboxEvent.builder()
                    .id(UUID.randomUUID())
                    .aggregateId(order.getId())
                    .aggregateType("ORDER")
                    .eventType("ORDER_CREATED")
                    .payload(objectMapper.writeValueAsString(orderCreatedEvent))
                    .status("NEW")
                    .createdAt(Instant.now())
                    .build();
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e + "error in createOrderFromCartEvent outbox event creation inside orderSevice createOrderFromCartEvent");
        }

        outboxEventRepository.save(outboxEvent);

        log.info("Order created from cart event. orderNumber={}",
                orderNumber);
    }

    private String serialize(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            log.error("Serialization failed", e);
            throw new BaseException("SERIALIZATION_ERROR",
                    "Failed to serialize response");
        }
    }

}