package com.ecom.cartservice.config;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
public class KafkaConfig {

    @Bean
    public DefaultErrorHandler errorHandler(
            KafkaTemplate<Object, Object> kafkaTemplate
    ) {

        // Sends failed messages to <topic>.DLT
        DeadLetterPublishingRecoverer recoverer =
                new DeadLetterPublishingRecoverer(kafkaTemplate);

        // Retry 3 times, 3 seconds gap
        FixedBackOff backOff = new FixedBackOff(
                3000L,  // 3 sec delay
                3       // 3 retries
        );

        return new DefaultErrorHandler(recoverer, backOff);
    }
}