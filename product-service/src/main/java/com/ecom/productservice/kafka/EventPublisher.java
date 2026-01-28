package com.ecom.productservice.kafka;

import com.ecom.events.base.DomainEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class EventPublisher {

    private final KafkaTemplate<String, DomainEvent> kafkaTemplate;

    public void publish(String topic, String key, DomainEvent event) {

        // fill metadata centrally
        event.setEventId(UUID.randomUUID());
        event.setOccurredAt(Instant.now());
        event.setEventVersion(1);

        kafkaTemplate.send(topic, key, event);
    }
}
