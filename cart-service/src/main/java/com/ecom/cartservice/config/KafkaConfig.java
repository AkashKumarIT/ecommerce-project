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
    public DefaultErrorHandler errorHandler(KafkaTemplate<Object, Object> kafkaTemplate) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate);

        // ✅ FIX: 0L, 0L ka matlab hai - No retries for bad messages like 'cls'
        // Isse message turant skip hokar DLT mein chala jayega aur service crash nahi hogi
        return new DefaultErrorHandler(recoverer, new FixedBackOff(0L, 0L));
    }
}