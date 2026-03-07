package com.ecom.orderservice.kafka;

import com.ecom.orderservice.model.OutboxEvent;
import com.ecom.orderservice.repository.OutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxPublisher {

    private final OutboxEventRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelay = 5000)
    public void publishEvents() {

        List<OutboxEvent> events =
                outboxRepository.findTop20ByStatusOrderByCreatedAtAsc("NEW");

        for (OutboxEvent event : events) {

            try {
                String key = extractBusinessKey(event.getPayload(), event.getAggregateId());

                kafkaTemplate.send(
                        "order-events",
                        key,
                        event.getPayload()
                ).get(5, TimeUnit.SECONDS);

                event.setStatus("PUBLISHED");
                event.setPublishedAt(Instant.now());

                outboxRepository.save(event);

            } catch (Exception e) {
                log.error("Failed to publish order outbox event id={}", event.getId(), e);
            }
        }
    }

    private String extractBusinessKey(String payload, Long fallbackKey) {
        try {
            String orderId = objectMapper.readTree(payload).path("orderId").asText("");
            if (!orderId.isBlank()) {
                return orderId;
            }

            String cartId = objectMapper.readTree(payload).path("cartId").asText("");
            if (!cartId.isBlank()) {
                return cartId;
            }
        } catch (Exception ex) {
            log.warn("Unable to parse outbox payload for key extraction", ex);
        }

        return String.valueOf(fallbackKey);
    }
}