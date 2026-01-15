package com.ecom.productservice.productServices;

import com.ecom.productservice.exception.ProductNotFoundException;
import com.ecom.productservice.mapper.ProductMapper;
import com.ecom.productservice.productDto.ProductRequest;
import com.ecom.productservice.productDto.ProductResponse;
import com.ecom.productservice.productDto.RestPage;
import com.ecom.productservice.productModel.Product;
import com.ecom.productservice.productRepository.ProductRepository;
import com.ecom.productservice.productRepository.ProductSpecification;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Cacheable(
            value = "productSearchPage",
            key = "T(java.util.Objects).toString(#search,'')" +
                    " + '|' + T(java.util.Objects).toString(#category,'')" +
                    " + '|' + T(java.util.Objects).toString(#minPrice,'')" +
                    " + '|' + T(java.util.Objects).toString(#maxPrice,'')" +
                    " + '|' + #page + '|' + #size + '|' + #sortBy + '|' + #direction"
    )
    public Page<ProductResponse> getAllProducts(String search,
                                                String category,
                                                Double minPrice,
                                                Double maxPrice,
                                                int page,
                                                int size, String sortBy, String direction){
        // Create pagination and sorting information
        Sort sort = direction.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);

        // create specification (dynamic query building)
        Specification<Product> spec = Specification.allOf(
                ProductSpecification.globalSearch(search),
                ProductSpecification.hasCategory(category),
                ProductSpecification.hasPriceBetween(minPrice, maxPrice));

        // 1. Pehle Database se Page nikalo
        Page<Product> productPage = productRepository.findAll(spec, pageable);

        // 2. Phir DTO list banao
        List<ProductResponse> responseList = productPage.stream()
                .map(ProductMapper::toResponse)
                .collect(Collectors.toList());

        // 3. ✅ MAGIC FIX: PageImpl ki jagah RestPage return karo
        // Ye class Redis mein save ho sakti hai bina error ke
        return new RestPage<>(responseList, pageable, productPage.getTotalElements());
    }


    public List<ProductResponse> createProducts(List<ProductRequest> requests){
        List<Product> products = requests.stream()
                .map(req -> Product.builder()
                        .name(req.getName())
                        .description(req.getDescription())
                        .price(req.getPrice())
                        .quantity(req.getQuantity())
                        .imageUrl(req.getImageUrl())
                        .category(req.getCategory())
                        .status(true)
                        .build())
                .collect(Collectors.toList());

        List<Product> savedProducts = productRepository.saveAll(products);

        return savedProducts.stream()
                .map(ProductMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Cacheable(value = "productById", key = "#id")
    public ProductResponse getProductById(UUID id){
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException("Product with id " + id + " not found"));

        return ProductMapper.toResponse(product);
    }


    @CacheEvict(value = "productById", key = "#id")
    public void deleteProduct(UUID id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException("Product with id " + id + " not found"));

        productRepository.delete(product);
    }

    @CacheEvict(value = "productById", key = "#id")
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
