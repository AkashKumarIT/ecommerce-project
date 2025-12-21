package com.ecom.productservice.productRepository;

import com.ecom.productservice.productModel.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

// Added JpaSpecificationExecutor to enable dynamic filtering
public interface ProductRepository extends JpaRepository<Product, UUID>, JpaSpecificationExecutor<Product> {
    // We don't need all those manual findBy... methods anymore!
    // The Specification Executor handles them all.
}
