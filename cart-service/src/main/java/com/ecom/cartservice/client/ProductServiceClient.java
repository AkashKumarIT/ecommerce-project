package com.ecom.cartservice.client;

import com.ecom.cartservice.dto.ProductResponse;
import com.ecom.cartservice.exception.BaseException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductServiceClient {
    private final WebClient.Builder webClientBuilder;

    private static final String PRODUCT_SERVICE_BASE_URL = "http://product-service";

    public ProductResponse getProduct(UUID productId, String token) {
        try {
            return webClientBuilder.build()
                    .get()
                    .uri(PRODUCT_SERVICE_BASE_URL + "/api/products/{id}", productId)
                    .headers(headers -> {
                        if (token != null && !token.isBlank()) {
                            headers.setBearerAuth(token);
                        }
                    })
                    .retrieve()
                    // FIX: Use status.is4xxClientError() on the status object itself
                    .onStatus(status -> status.value() == 404,
                            response -> Mono.error(
                                    new BaseException("PRODUCT_NOT_FOUND",
                                            "Product not found")))
                    // FIX: Use status.is5xxServerError()
                    .onStatus(HttpStatusCode::is5xxServerError,
                            response -> Mono.error(
                                    new BaseException("PRODUCT_SERVICE_ERROR",
                                            "Product service unavailable")))
                    .bodyToMono(ProductResponse.class)
                    .block();
        } catch (Exception ex) {
            log.error("Failed to fetch product from product-service: {}", productId, ex);
            throw new RuntimeException("Unable to fetch product");
        }
    }
}
