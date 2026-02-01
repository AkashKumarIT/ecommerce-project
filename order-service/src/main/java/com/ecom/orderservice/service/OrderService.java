package com.ecom.orderservice.service;

import com.ecom.orderservice.client.InventoryClient;
import com.ecom.orderservice.dto.InventoryReservationRequest;
import com.ecom.orderservice.dto.OrderRequest;
import com.ecom.orderservice.mappers.Mapper;
import com.ecom.orderservice.model.Order;
import com.ecom.orderservice.model.OrderLineItems;
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
    private final InventoryClient inventoryClient;
    private final Mapper mapper;

    public String placeOrder(OrderRequest orderRequest) {
        Order order = new Order();
        String orderNumber = UUID.randomUUID().toString();
        order.setOrderNumber(orderNumber);

        log.info("at convert DTO to Entity");
        // 1. Convert DTO to Entity
        List<OrderLineItems> orderLineItems = orderRequest.getOrderLineItemsDtoList()
                .stream()
                .map(mapper::mapToDto)
                .toList();

        order.setOrderLineItemsList(orderLineItems);


        // --- NEW RESERVATION LOGIC START ---
        log.info("Preparing Reservation Request");

        // 2. Prepare the Reservation Request DTO
        List<InventoryReservationRequest.Item> reservationItems = order.getOrderLineItemsList().stream()
                .map(item -> InventoryReservationRequest.Item.builder()
                        .sku(item.getSku())
                        .qty(item.getQuantity())
                        .build())
                .toList();

        InventoryReservationRequest reservationRequest = InventoryReservationRequest.builder()
                .orderId(orderNumber)
                .items(reservationItems)
                .build();

        // 3. Call Inventory Service to RESERVE
        log.info("Calling Inventory Service to Reserve Stock...");

        // This call does two things:
        inventoryClient.reserveInventory(reservationRequest);

        // --- RESERVATION LOGIC END ---

        // 4. If the code reaches here, it means Reservation was SUCCESSFUL.
        log.info("Reservation Successful. Saving Order.");

        order.setStatus("PLACED");
        orderRepository.save(order);

        log.info("Order Placed Successfully: {}", order.getOrderNumber());
        return "Order Placed Successfully";
    }
}