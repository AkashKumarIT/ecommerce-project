package com.ecom.orderservice.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

//    @Bean
//    public OpenAPI orderServiceOpenAPI() {
//        return new OpenAPI()
//                .info(new Info()
//                        .title("Order Service API")
//                        .version("v1")
//                        .description("Order creation, lifecycle, and cancellation APIs"));
//    }
@Bean
public OpenAPI customOpenAPI() {
    return new OpenAPI()
            .info(new Info()
                    .title("E-Commerce API")
                    .version("v1")
                    .description("APIs with JWT Authentication"))
            // 👇 Yahan se Security configuration start hoti hai
            .components(new Components()
                    .addSecuritySchemes("bearerAuth", new SecurityScheme()
                            .type(SecurityScheme.Type.HTTP)
                            .scheme("bearer")
                            .bearerFormat("JWT")
                            .in(SecurityScheme.In.HEADER)
                            .name("Authorization")))
            .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
}
}
