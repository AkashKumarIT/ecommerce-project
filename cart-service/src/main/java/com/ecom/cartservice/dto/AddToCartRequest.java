package com.ecom.cartservice.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.context.annotation.Bean;

import java.util.UUID;

@Data
public class AddToCartRequest {
    @NotNull
    private UUID productId;

    @Min(1)
    private Integer quantity;
}
