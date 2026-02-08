package com.ecom.orderservice.service;

import com.ecom.events.order.OrderCancelledEvent;
import com.ecom.events.order.OrderCreatedEvent;
import com.ecom.orderservice.dto.OrderRequest;
import com.ecom.orderservice.kafka.EventPublisher;
import com.ecom.orderservice.mappers.Mapper;
import com.ecom.orderservice.model.Order;
import com.ecom.orderservice.model.OrderLineItems;
import com.ecom.orderservice.model.OrderStatus;
import com.ecom.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final EventPublisher eventPublisher;
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
                items.stream()
                        .map(i -> new OrderCreatedEvent.OrderItem(
                                i.getSku(), i.getQuantity()))
                        .toList()
        );

        eventPublisher.publish("order-events", orderNumber, event);

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

        eventPublisher.publish(
                "order-events",
                orderNumber,
                new OrderCancelledEvent(orderNumber)
        );

        log.info("Order Cancelled Successfully: {}", orderNumber);
        return "Order Cancelled Successfully";
    }
}