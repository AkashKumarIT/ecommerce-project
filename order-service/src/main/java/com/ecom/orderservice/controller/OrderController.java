package com.ecom.orderservice.controller;

import com.ecom.orderservice.dto.OrderRequest;
import com.ecom.orderservice.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/order")
@RequiredArgsConstructor
@Slf4j
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public String placeOrder(@RequestBody OrderRequest orderRequest) {
        log.info("ORDER SERVICE controller - placeOrder called");
        return orderService.placeOrder(orderRequest);
    }
}