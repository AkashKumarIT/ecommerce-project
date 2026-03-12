package com.ecom.paymentservice.service;

import com.ecom.events.order.OrderCancelledEvent;
import com.ecom.events.order.OrderCreatedEvent;
import com.ecom.events.payment.PaymentCompletedEvent;
import com.ecom.events.payment.PaymentCreatedEvent;
import com.ecom.events.payment.PaymentFailedEvent;
import com.ecom.paymentservice.dto.PaymentResponse;
import com.ecom.paymentservice.entity.OutboxEvent;
import com.ecom.paymentservice.entity.Payment;
import com.ecom.paymentservice.entity.PaymentStatus;
import com.ecom.paymentservice.exception.ConflictException;
import com.ecom.paymentservice.exception.ResourceNotFoundException;
import com.ecom.paymentservice.provider.PaymentProvider;
import com.ecom.paymentservice.repository.OutboxEventRepository;
import com.ecom.paymentservice.repository.PaymentRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final PaymentProvider paymentProvider;
    private final ObjectMapper objectMapper;

    public void createPaymentForOrder(OrderCreatedEvent event) {
        if (paymentRepository.existsByOrderId(event.getOrderId())) {
            log.info("Payment already exists for orderId={}", event.getOrderId());
            return;
        }

        BigDecimal amount = event.getTotalAmount() != null ? event.getTotalAmount() : BigDecimal.ZERO;
        String currency = event.getCurrency() != null ? event.getCurrency() : "INR";

        Payment payment = Payment.builder()
                .id(UUID.randomUUID())
                .orderId(event.getOrderId())
                .cartId(event.getCartId())
                .userId(event.getUserId())
                .amount(amount)
                .currency(currency)
                .status(PaymentStatus.CREATED)
                .paymentProvider(paymentProvider.providerName())
                .providerPaymentId(paymentProvider.createPayment(event.getOrderId(), amount, currency))
                .orderItemsPayload(writeOrderItems(event.getItems()))
                .build();

        paymentRepository.save(payment);

        PaymentCreatedEvent createdEvent = new PaymentCreatedEvent(
                payment.getId(),
                payment.getOrderId(),
                payment.getCartId(),
                payment.getUserId(),
                payment.getAmount(),
                payment.getCurrency()
        );

        saveOutbox(payment.getId(), "PAYMENT_CREATED", createdEvent);
        log.info("Payment created paymentId={} orderId={} amount={} {}", payment.getId(), payment.getOrderId(), payment.getAmount(), payment.getCurrency());
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPayment(UUID paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found: " + paymentId));
        return PaymentResponse.from(payment);
    }

    public PaymentResponse completePayment(UUID paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found: " + paymentId));

        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            log.info("Idempotent complete called for paymentId={} already SUCCESS", paymentId);
            return PaymentResponse.from(payment);
        }

        if (payment.getStatus() == PaymentStatus.FAILED || payment.getStatus() == PaymentStatus.CANCELLED) {
            throw new ConflictException("Cannot complete payment in status: " + payment.getStatus());
        }

        if (payment.getStatus() == PaymentStatus.CREATED) {
            payment.setStatus(PaymentStatus.PENDING);
        }

        if (!paymentProvider.verifyPayment(payment.getProviderPaymentId())) {
            throw new ConflictException("Payment provider verification failed");
        }

        payment.setStatus(PaymentStatus.SUCCESS);
        paymentRepository.save(payment);

        PaymentCompletedEvent completedEvent = new PaymentCompletedEvent(
                payment.getId(),
                payment.getOrderId(),
                payment.getCartId(),
                payment.getUserId(),
                payment.getAmount(),
                payment.getCurrency(),
                readOrderItems(payment.getOrderItemsPayload())
        );

        saveOutbox(payment.getId(), "PAYMENT_COMPLETED", completedEvent);
        log.info("Payment completed paymentId={} orderId={}", paymentId, payment.getOrderId());

        return PaymentResponse.from(payment);
    }

    public PaymentResponse failPayment(UUID paymentId, String reason) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found: " + paymentId));

        if (payment.getStatus() == PaymentStatus.FAILED) {
            log.info("Idempotent fail called for paymentId={} already FAILED", paymentId);
            return PaymentResponse.from(payment);
        }

        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            throw new ConflictException("Cannot fail payment in status SUCCESS");
        }

        payment.setStatus(PaymentStatus.FAILED);
        paymentRepository.save(payment);

        PaymentFailedEvent failedEvent = new PaymentFailedEvent(
                payment.getId(),
                payment.getOrderId(),
                payment.getCartId(),
                payment.getUserId(),
                payment.getAmount(),
                payment.getCurrency(),
                reason == null || reason.isBlank() ? "PAYMENT_MARKED_FAILED" : reason
        );

        saveOutbox(payment.getId(), "PAYMENT_FAILED", failedEvent);
        log.warn("Payment failed paymentId={} orderId={} reason={}", paymentId, payment.getOrderId(), failedEvent.getReason());

        return PaymentResponse.from(payment);
    }

    public void cancelPaymentForOrder(OrderCancelledEvent event) {

        paymentRepository.findByOrderId(event.getOrderId())
                .ifPresent(payment -> {

                    // ✅ YEH HAI ASLI REFUND LOGIC
                    if (payment.getStatus() == PaymentStatus.SUCCESS) {
                        payment.setStatus(PaymentStatus.REFUNDED); // SUCCESS ko REFUNDED me badlo
                        paymentRepository.save(payment);
                        log.info(
                                "Payment REFUNDED due to order cancellation paymentId={} orderId={}",
                                payment.getId(),
                                payment.getOrderId()
                        );
                        return;
                    }

                    if (payment.getStatus() == PaymentStatus.CANCELLED || payment.getStatus() == PaymentStatus.FAILED) {
                        return; // Already cancel/fail ho chuka hai toh kuch mat karo
                    }

                    // Agar payment abhi pending/created thi, toh bas CANCEL kar do (paisa nahi kata)
                    payment.setStatus(PaymentStatus.CANCELLED);
                    paymentRepository.save(payment);

                    log.info(
                            "Payment safely CANCELLED (No money deducted) for paymentId={} orderId={}",
                            payment.getId(),
                            payment.getOrderId()
                    );
                });
    }

    private void saveOutbox(UUID paymentId, String eventType, Object payload) {
        try {
            OutboxEvent outbox = OutboxEvent.builder()
                    .id(UUID.randomUUID())
                    .aggregateId(paymentId)
                    .aggregateType("PAYMENT")
                    .eventType(eventType)
                    .payload(objectMapper.writeValueAsString(payload))
                    .status("NEW")
                    .createdAt(Instant.now())
                    .build();

            outboxEventRepository.save(outbox);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to create payment outbox event", e);
        }
    }

    private String writeOrderItems(List<OrderCreatedEvent.OrderItem> items) {
        try {
            return objectMapper.writeValueAsString(items);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize order items", e);
        }
    }

    private List<PaymentCompletedEvent.Item> readOrderItems(String payload) {
        if (payload == null || payload.isBlank()) {
            return List.of();
        }

        try {
            List<OrderCreatedEvent.OrderItem> items = objectMapper.readValue(
                    payload,
                    new TypeReference<List<OrderCreatedEvent.OrderItem>>() {}
            );
            return items.stream()
                    .map(i -> new PaymentCompletedEvent.Item(i.getSku(), i.getQty()))
                    .toList();
        } catch (Exception ex) {
            log.error("Unable to deserialize order items from payment payload", ex);
            return List.of();
        }
    }

    @Transactional(readOnly = true)
    public PaymentResponse paymentByOrderId(String orderId){
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Payment not found for orderId: " + orderId)
                );
        return PaymentResponse.from(payment);
    }
}
