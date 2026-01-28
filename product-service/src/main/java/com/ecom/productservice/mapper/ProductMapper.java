package com.ecom.productservice.mapper;

import com.ecom.productservice.productDto.ProductResponse;
import com.ecom.productservice.productModel.Product;

public class ProductMapper {

    public static ProductResponse toResponse(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .sku(product.getSku())
                .description(product.getDescription())
                .price(product.getPrice())
                .quantity(product.getQuantity())
                .imageUrl(product.getImageUrl())
                .category(product.getCategory())
                .status(product.isStatus())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }

    public static Product toEntity(ProductResponse productResponse) {
        return Product.builder()
                .id(productResponse.getId())
                .name(productResponse.getName())
                .sku(productResponse.getSku())
                .description(productResponse.getDescription())
                .price(productResponse.getPrice())
                .quantity(productResponse.getQuantity())
                .imageUrl(productResponse.getImageUrl())
                .category(productResponse.getCategory())
                .status(productResponse.getStatus() != null ? productResponse.getStatus() : true)
                .createdAt(productResponse.getCreatedAt())
                .updatedAt(productResponse.getUpdatedAt())
                .build();
    }
}
