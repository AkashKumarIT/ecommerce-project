package com.ecom.productservice.productDto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class ProductResponse {

    private UUID id;
    private String name;
    private String description;
    private Double price;
    private Integer quantity;
    private String imageUrl;
    private String category;
    private Boolean status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
