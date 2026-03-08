package com.ecom.paymentservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
public class KafkaConfig {

    @Bean
    public DefaultErrorHandler errorHandler() {
        // Ye 'SerializationException' ko bhi handle karega aur message skip kar dega
        DefaultErrorHandler handler = new DefaultErrorHandler(new FixedBackOff(0L, 0L));
        handler.addNotRetryableExceptions(jakarta.validation.ValidationException.class);
        // Isse application crash nahi hogi, bas log karke aage badh jayegi
        return handler;
    }
}
