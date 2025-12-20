package com.ecom.productservice.productServices;

import com.ecom.productservice.exception.ProductNotFoundException;
import com.ecom.productservice.mapper.ProductMapper;
import com.ecom.productservice.productDto.ProductRequest;
import com.ecom.productservice.productDto.ProductResponse;
import com.ecom.productservice.productModel.Product;
import com.ecom.productservice.productRepository.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public Page<ProductResponse> getAllProductPaginated(int page, int size, String sortBy, String direction){
        Sort sort = direction.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);
        return productRepository.findAll(pageable)
                .map(ProductMapper :: toResponse);
    }

    public Page<ProductResponse> getProductsByCategoryPaginated(String category, int page, int size, String sortBy, String direction){
        Sort sort = direction.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Product> productPage = productRepository.findByCategoryIgnoreCase(category, pageable);
        return productPage.map(ProductMapper :: toResponse);
    }

    public List<ProductResponse> searchProductsByName(String name) {
        return productRepository.findByNameContainingIgnoreCase(name)
                .stream().map(ProductMapper :: toResponse)
                .toList();
    }

    public List<ProductResponse> filterProductByCategory(String category) {
        return productRepository.findByCategoryIgnoreCase(category)
                .stream().map(ProductMapper :: toResponse)
                .toList();
    }

    public List<ProductResponse> filterProductByPrice(Double minPrice, Double maxPrice) {
        return productRepository.findByPriceBetween(minPrice, maxPrice)
                .stream().map(ProductMapper :: toResponse)
                .toList();
    }

    public List<ProductResponse> filterProductByCategoryAndPrice(String category, Double minPrice, Double maxPrice) {
        return productRepository.findByCategoryIgnoreCaseAndPriceBetween(category, minPrice, maxPrice)
                .stream().map(ProductMapper :: toResponse)
                .toList();
    }

    public ProductResponse createProduct(ProductRequest req){
        Product product = Product.builder()
                .name(req.getName())
                .description(req.getDescription())
                .price(req.getPrice())
                .quantity(req.getQuantity())
                .imageUrl(req.getImageUrl())
                .category(req.getCategory())
                .status(true)
                .build();

        productRepository.save(product);
        return ProductMapper.toResponse(product);
    }

    public List<ProductResponse> getAllProducts(){
        return productRepository.findAll()
                .stream()
                .map(ProductMapper :: toResponse)
                .toList();
    }

    public ProductResponse getProductById(UUID id){
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException("Product with id " + id + " not found"));

        return ProductMapper.toResponse(product);
    }

    public void deleteProduct(UUID id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException("Product with id " + id + " not found"));

        productRepository.delete(product);
    }

    public ProductResponse updateProduct(UUID id, ProductRequest req) {
        Product existingProduct = ProductMapper.toEntity(getProductById(id));

        existingProduct.setName(req.getName());
        existingProduct.setDescription(req.getDescription());
        existingProduct.setPrice(req.getPrice());
        existingProduct.setQuantity(req.getQuantity());
        existingProduct.setImageUrl(req.getImageUrl());
        existingProduct.setCategory(req.getCategory());

        productRepository.save(existingProduct);
        return ProductMapper.toResponse(existingProduct);
    }

}
