package com.ecom.cartservice.dto;

import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class UpdateQuantityRequest {
    @Min(value = 0, message = "Quantity cannot be negative")
    private Integer quantity;
}
