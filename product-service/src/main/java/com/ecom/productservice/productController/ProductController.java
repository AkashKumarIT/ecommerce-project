package com.ecom.productservice.productController;

import com.ecom.productservice.productDto.ProductRequest;
import com.ecom.productservice.productDto.ProductResponse;
import com.ecom.productservice.productModel.Product;
import com.ecom.productservice.productServices.ProductService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.*;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;
    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/hello")
    public String hello() {
        return "Hello from Product Service";
    }
    // 🌟 UNIFIED ENDPOINT
    // Valid Calls:
    // 1. GET /api/products (Get All)
    // 2. GET /api/products?search=iphone (Search)
    // 3. GET /api/products?category=mobile&minPrice=50000 (Filter)
    // 4. GET /api/products?search=samsung&sort=price&direction=asc (Search + Sort)
    @GetMapping
    public ResponseEntity<Page<ProductResponse>> getProducts(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction
    ) {
        Page<ProductResponse> products = productService
                .getAllProducts(search, category, minPrice, maxPrice, page, size, sortBy, direction);
        return ResponseEntity.ok(products);
    }


    @PreAuthorize("hasRole('ADMIN')") // Only admins can create products
    @PostMapping // I recommend adding a specific path like '/bulk' or just use '/'
    public ResponseEntity<List<ProductResponse>> createProducts(@RequestBody List<ProductRequest> requests) {
        List<ProductResponse> responses = productService.createProducts(requests);
        return ResponseEntity.status(HttpStatus.CREATED).body(responses);
    }


    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProductById(@PathVariable UUID id) {
        ProductResponse product = productService.getProductById(id);
        return ResponseEntity.ok(product);
    }



    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<ProductResponse> updateProduct(@PathVariable UUID id, @Valid @RequestBody ProductRequest req) {
        ProductResponse updatedProduct = productService.updateProduct(id, req);
        return ResponseEntity.ok(updatedProduct);
    }


    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable UUID id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }



}
