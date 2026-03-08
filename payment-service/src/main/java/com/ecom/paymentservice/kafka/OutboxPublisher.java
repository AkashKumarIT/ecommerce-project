package com.ecom.paymentservice.kafka;

import com.ecom.paymentservice.entity.OutboxEvent;
import com.ecom.paymentservice.repository.OutboxEventRepository;
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

    @Scheduled(fixedDelay = 5000)
    public void publishEvents() {
        List<OutboxEvent> events = outboxRepository.findTop20ByStatusOrderByCreatedAtAsc("NEW");

        for (OutboxEvent event : events) {
            kafkaTemplate.send(
                    KafkaTopics.PAYMENT_EVENTS,
                    event.getAggregateId().toString(),
                    event.getPayload()
            ).whenComplete((result, ex) -> {

                if (ex != null) {
                    log.error(
                            "Failed to publish payment outbox event id={}",
                            event.getId(),
                            ex
                    );
                    return;
                }

                event.setStatus("PUBLISHED");
                event.setPublishedAt(Instant.now());

                outboxRepository.save(event);

                log.info(
                        "Published payment outbox event id={} type={}",
                        event.getId(),
                        event.getEventType()
                );
            });
        }
    }
}
