package com.ecom.productservice;

import java.util.TimeZone;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.kafka.annotation.EnableKafka;

@SpringBootApplication
@EnableJpaAuditing
@EnableCaching
@EnableKafka
public class ProductserviceApplication {

	public static void main(String[] args) {

		TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
		System.out.println("Time zone set to UTC");
		SpringApplication.run(ProductserviceApplication.class, args);
	}

	@Bean
	public CommandLineRunner clearCache(RedisConnectionFactory connectionFactory) {
		return args -> {
			connectionFactory.getConnection().serverCommands().flushAll();
			System.out.println("---------------------------------------");
			System.out.println("🧹 Redis Cache Cleared Successfully! 🧹");
			System.out.println("---------------------------------------");
		};
	}

}
