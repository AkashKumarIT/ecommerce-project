package com.ecom.paymentservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.TimeZone;

@SpringBootTest(properties = {
        "eureka.client.enabled=false",
        "spring.cloud.discovery.enabled=false",
        "spring.datasource.url=jdbc:postgresql://localhost:5438/paymentdb",
        "spring.datasource.username=payment_user",
        "spring.datasource.password=payment_pass",
        "spring.datasource.driver-class-name=org.postgresql.Driver",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect"
})
class PaymentServiceApplicationTests {

    static {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    @Test
    void contextLoads() {
    }
}
