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

    @GetMapping("/search")
    public ResponseEntity<List<ProductResponse>> searchProductsByName(@RequestParam String name) {
        List<ProductResponse> products = productService.searchProductsByName(name);
        return ResponseEntity.ok(products);
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<List<ProductResponse>> filterProductByCategory(@PathVariable String category) {
        List<ProductResponse> products = productService.filterProductByCategory(category);
        return ResponseEntity.ok(products);
    }

    @GetMapping("/price")
    public ResponseEntity<List<ProductResponse>> filterProductByPrice(@RequestParam Double minPrice, @RequestParam Double maxPrice) {
        List<ProductResponse> products = productService.filterProductByPrice(minPrice, maxPrice);
        return ResponseEntity.ok(products);
    }

    @GetMapping("/filter")
    public ResponseEntity<List<ProductResponse>> filterProductByCategoryAndPrice(@RequestParam String category, @RequestParam Double minPrice, @RequestParam Double maxPrice) {
        List<ProductResponse> products = productService.filterProductByCategoryAndPrice(category, minPrice, maxPrice);
        return ResponseEntity.ok(products);
    }

    @PostMapping
    public ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody ProductRequest req) {
        ProductResponse product = productService.createProduct(req);
        return ResponseEntity.created(URI.create("/api/products/" + product.getId())).body(product);
    }

    @GetMapping
    public ResponseEntity<List<ProductResponse>> getAllProducts() {
        return ResponseEntity.ok(productService.getAllProducts());
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

    @GetMapping("/paginated")
    public Page<ProductResponse> getProductAllPaginated(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction
    ) {
        return productService.getAllProductPaginated(page, size, sortBy, direction);
    }

    @GetMapping("/category/{category}/paginated")
    public Page<ProductResponse> getProductsByCategoryPaginated(
            @PathVariable String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction
    ) {
        return productService.getProductsByCategoryPaginated(category, page, size, sortBy, direction);
    }

}
