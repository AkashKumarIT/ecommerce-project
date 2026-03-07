package com.ecom.cartservice.client;

import com.ecom.cartservice.dto.InventoryCheckResponse;
import com.ecom.cartservice.dto.InventoryResponse;
import com.ecom.cartservice.exception.BaseException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class InventoryServiceClient {

    private final WebClient.Builder webClientBuilder;

    private static final String INVENTORY_SERVICE = "http://inventory-service";

    public InventoryResponse getInventory(String sku) {

        List<InventoryResponse> response =
                webClientBuilder.baseUrl(INVENTORY_SERVICE)
                        .build()
                        .get()
                        .uri(uriBuilder -> uriBuilder
                                .path("/api/inventory")
                                .queryParam("sku", sku)
                                .build())
                        .retrieve()
                        .bodyToFlux(InventoryResponse.class)
                        .collectList()
                        .block();

        if (response == null || response.isEmpty()) {
            throw new BaseException("STOCK_NOT_FOUND",
                    "Stock not found for SKU: " + sku);
        }

        return response.get(0);
    }
}

