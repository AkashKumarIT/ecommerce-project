package com.ecom.productservice.productController;

import com.ecom.productservice.productDto.ProductRequest;
import com.ecom.productservice.productDto.ProductResponse;
import com.ecom.productservice.productModel.Product;
import com.ecom.productservice.productServices.ProductService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;
    public ProductController(ProductService productService) {
        this.productService = productService;
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



    @PostMapping
    public ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody ProductRequest req) {
        ProductResponse product = productService.createProduct(req);
        return ResponseEntity.created(URI.create("/api/products/" + product.getId())).body(product);
    }


    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProductById(@PathVariable UUID id) {
        ProductResponse product = productService.getProductById(id);
        return ResponseEntity.ok(product);
    }



    @PutMapping("/{id}")
    public ResponseEntity<ProductResponse> updateProduct(@PathVariable UUID id, @Valid @RequestBody ProductRequest req) {
        ProductResponse updatedProduct = productService.updateProduct(id, req);
        return ResponseEntity.ok(updatedProduct);
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable UUID id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }



}
