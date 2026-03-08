package com.ecom.paymentservice.controller;

import com.ecom.paymentservice.dto.PaymentResponse;
import com.ecom.paymentservice.service.PaymentService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Slf4j
@Validated
public class PaymentController {

    private final PaymentService paymentService;

    @GetMapping("/{id}")
    public PaymentResponse getPayment(@PathVariable UUID id) {
        return paymentService.getPayment(id);
    }

    @PostMapping("/{id}/complete")
    public PaymentResponse completePayment(@PathVariable UUID id) {
        log.info("Received request to complete paymentId={}", id);
        return paymentService.completePayment(id);
    }

    @PostMapping("/{id}/fail")
    public PaymentResponse failPayment(
            @PathVariable UUID id,
            @RequestBody(required = false) Map<String, String> body
    ) {
        String reason = body != null ? body.getOrDefault("reason", "MANUAL_FAILURE") : "MANUAL_FAILURE";
        log.info("Received request to fail paymentId={} reason={}", id, reason);
        return paymentService.failPayment(id, reason);
    }

    @GetMapping("/order/{orderId}")
    public PaymentResponse paymentByOrderId(@PathVariable String orderId){
        log.info("Fetching payment for orderId={}", orderId);
        return paymentService.paymentByOrderId(orderId);
    }
}
