package com.ecom.paymentservice.service;

import com.ecom.events.payment.PaymentFailedEvent;
import com.ecom.paymentservice.entity.Payment;
import com.ecom.paymentservice.entity.PaymentStatus;
import com.ecom.paymentservice.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentTimeoutService {

    private final PaymentRepository paymentRepository;
    private final PaymentService paymentService;

    private static final Duration PAYMENT_TIMEOUT = Duration.ofMinutes(15);

    @Scheduled(fixedDelay = 60000)
    public void cancelExpiredPayments() {

        Instant expiryTime = Instant.now().minus(PAYMENT_TIMEOUT);

        List<Payment> expiredPayments =
                paymentRepository.findByStatusInAndCreatedAtBefore(
                        List.of(PaymentStatus.CREATED, PaymentStatus.PENDING),
                        expiryTime
                );

        for (Payment payment : expiredPayments) {

            log.warn(
                    "Payment timeout detected paymentId={} orderId={}",
                    payment.getId(),
                    payment.getOrderId()
            );

            paymentService.failPayment(
                    payment.getId(),
                    "PAYMENT_TIMEOUT"
            );
        }
    }
}