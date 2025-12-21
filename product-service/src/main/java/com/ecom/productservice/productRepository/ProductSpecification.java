package com.ecom.productservice.productRepository;

import com.ecom.productservice.productModel.Product;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class ProductSpecification {
    public static Specification<Product> globalSearch(String searchText){
        return (root, query, criteriaBuilder) -> {
            if (!StringUtils.hasText(searchText)) return null;

            String text = searchText.trim().toLowerCase();

            // Step 1: Tokenization (String ko words mein todo)
            // "Samsung Mobile" -> ["Samsung", "Mobile"]
            String[] keywords = text.split("\\s+");

            List<Predicate> finalPredicates = new ArrayList<>();

            for (String keyword : keywords) {
                // Har keyword ke liye check karo: Kya ye Name ya Description ya Category mein hai?
                Predicate keywordInName = criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), "%" + keyword + "%");
                Predicate keywordInDesc = criteriaBuilder.like(criteriaBuilder.lower(root.get("description")), "%" + keyword + "%");
                Predicate keywordInCategory = criteriaBuilder.like(criteriaBuilder.lower(root.get("category")), "%" + keyword + "%");

                // OR Logic: Keyword kahin bhi mil jaye
                Predicate keywordMatch = criteriaBuilder.or(keywordInName, keywordInDesc, keywordInCategory);

                // List mein add karo (Taaki baad mein sabko AND kar sakein)
                finalPredicates.add(keywordMatch);
            }

            // Step 2: Combine all keywords with AND
            // Matlab: Product mein "Samsung" BHI hona chahiye AUR "Mobile" BHI hona chahiye
            return criteriaBuilder.and(finalPredicates.toArray(new Predicate[0]));
        };
    }

    // ... Baaki purane methods (hasPriceBetween etc.) same rahenge ...

    public static Specification<Product> hasCategory(String category) {
        return (root, query, criteriaBuilder) -> {
            if (!StringUtils.hasText(category)) return null;
            return criteriaBuilder.equal(criteriaBuilder.lower(root.get("category")), category.toLowerCase());
        };
    }

    public static Specification<Product> hasPriceBetween(Double minPrice, Double maxPrice) {
        return (root, query, criteriaBuilder) -> {
            if (minPrice == null && maxPrice == null) return null;
            if (minPrice != null && maxPrice != null) {
                return criteriaBuilder.between(root.get("price"), minPrice, maxPrice);
            } else if (minPrice != null) {
                return criteriaBuilder.greaterThanOrEqualTo(root.get("price"), minPrice);
            } else {
                return criteriaBuilder.lessThanOrEqualTo(root.get("price"), maxPrice);
            }
        };
    }
}
