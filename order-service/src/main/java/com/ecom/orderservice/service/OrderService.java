package com.ecom.orderservice.service;

import com.ecom.events.cart.CartCheckoutInitiatedEvent;
import com.ecom.events.order.OrderCancelledEvent;
import com.ecom.events.order.OrderCreatedEvent;
import com.ecom.orderservice.dto.OrderRequest;
import com.ecom.orderservice.mappers.Mapper;
import com.ecom.orderservice.model.Order;
import com.ecom.orderservice.model.OrderLineItems;
import com.ecom.orderservice.model.OrderStatus;
import com.ecom.orderservice.model.OutboxEvent;
import com.ecom.orderservice.repository.OrderRepository;
import com.ecom.orderservice.repository.OutboxEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;
    private final Mapper mapper;

    public String placeOrder(OrderRequest request) {
        String orderNumber = UUID.randomUUID().toString();

        Order order = new Order();
        order.setOrderNumber(orderNumber);
        order.setStatus(OrderStatus.PENDING_PAYMENT);
        order.setCartId(UUID.randomUUID());

        List<OrderLineItems> items = request.getOrderLineItemsDtoList()
                .stream()
                .map(mapper::mapToDto)
                .toList();

        order.setOrderLineItemsList(items);
        orderRepository.save(order);

        OrderCreatedEvent createdEvent = new OrderCreatedEvent(
                orderNumber,
                order.getCartId(),
                order.getUserId(),
                calculateTotal(items),
                "INR",
                items.stream()
                        .map(i -> new OrderCreatedEvent.OrderItem(i.getSku(), i.getQuantity()))
                        .toList()
        );

        saveOutboxEvent(order, "ORDER_CREATED", createdEvent);
        log.info("Order placed orderNumber={} status={}", orderNumber, order.getStatus());
        return orderNumber;
    }

    public String cancelOrder(String orderNumber) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new com.ecom.orderservice.exception.ResourceNotFoundException("Order not found: " + orderNumber));

        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);

        OrderCancelledEvent cancelledEvent = new OrderCancelledEvent(
                orderNumber,
                order.getCartId(),
                "MANUAL_CANCEL"
        );

        saveOutboxEvent(order, "ORDER_CANCELLED", cancelledEvent);

        log.info("Order cancelled successfully: {}", orderNumber);
        return "Order cancelled successfully";
    }

    public void createOrderFromCartEvent(CartCheckoutInitiatedEvent event) {
        if (orderRepository.existsByCartId(event.getCartId())) {
            log.info("Order already exists for cart {}", event.getCartId());
            return;
        }

        String orderNumber = UUID.randomUUID().toString();

        Order order = new Order();
        order.setOrderNumber(orderNumber);
        order.setStatus(OrderStatus.PENDING_PAYMENT);
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

        OrderCreatedEvent orderCreatedEvent = new OrderCreatedEvent(
                orderNumber,
                event.getCartId(),
                event.getUserId(),
                event.getTotalAmount() != null ? event.getTotalAmount() : calculateTotal(items),
                "INR",
                items.stream()
                        .map(i -> new OrderCreatedEvent.OrderItem(i.getSku(), i.getQuantity()))
                        .toList()
        );

        saveOutboxEvent(order, "ORDER_CREATED", orderCreatedEvent);

        log.info("Order created from cart event. orderNumber={} cartId={}", orderNumber, event.getCartId());
    }

    private BigDecimal calculateTotal(List<OrderLineItems> items) {
        return items.stream()
                .map(i -> {
                    BigDecimal price = i.getPrice() != null ? i.getPrice() : BigDecimal.ZERO;
                    Integer quantity = i.getQuantity() != null ? i.getQuantity() : 0;
                    return price.multiply(BigDecimal.valueOf(quantity));
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
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

