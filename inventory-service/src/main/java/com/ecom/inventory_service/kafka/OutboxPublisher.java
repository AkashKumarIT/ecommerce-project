package com.ecom.inventory_service.kafka;

import com.ecom.inventory_service.model.OutboxEvent;
import com.ecom.inventory_service.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxPublisher {

    private final OutboxEventRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Scheduled(fixedDelay = 5000)
    public void publishEvents() {

        List<OutboxEvent> events =
                outboxRepository.findTop20ByStatusOrderByCreatedAtAsc("NEW");

        for (OutboxEvent event : events) {

            try {

                kafkaTemplate.send(
                        "inventory-events",
                        event.getAggregateId().toString(),
                        event.getPayload()
                );

                event.setStatus("PUBLISHED");
                event.setPublishedAt(Instant.now());

                outboxRepository.save(event);

            } catch (Exception e) {
                log.error("Failed to inventory event", e);
            }
        }
    }
}

