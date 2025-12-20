package com.ecom.productservice.productRepository;

import com.ecom.productservice.productModel.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {
    // 1. Search by name
    List<Product> findByNameContainingIgnoreCase(String name);

    // 2. Find by category
    List<Product> findByCategoryIgnoreCase(String category);

    Page<Product> findByCategoryIgnoreCase(String category, Pageable pageable);

    // 3. Filter by price range
    List<Product> findByPriceBetween(Double minPrice, Double maxPrice);

    List<Product> findByCategoryIgnoreCaseAndPriceBetween(String category, Double minPrice, Double maxPrice);
}
