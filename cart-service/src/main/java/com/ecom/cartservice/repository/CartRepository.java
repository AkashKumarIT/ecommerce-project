package com.ecom.cartservice.repository;

import com.ecom.cartservice.model.Cart;
import com.ecom.cartservice.model.CartStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface CartRepository extends JpaRepository<Cart, UUID> {

    // 🔥 ALWAYS fetch items for active USER cart
    @Query("""
           SELECT c FROM Cart c
           LEFT JOIN FETCH c.items
           WHERE c.userId = :userId
           AND c.status = :status
           """)
    Optional<Cart> findActiveUserCartWithItems(
            @Param("userId") String userId,
            @Param("status") CartStatus status
    );

    // 🔥 ALWAYS fetch items for active GUEST cart
    @Query("""
           SELECT c FROM Cart c
           LEFT JOIN FETCH c.items
           WHERE c.guestId = :guestId
           AND c.status = :status
           """)
    Optional<Cart> findActiveGuestCartWithItems(
            @Param("guestId") String guestId,
            @Param("status") CartStatus status
    );

    // Keep this for direct id-based fetch
    @Query("""
           SELECT c FROM Cart c
           LEFT JOIN FETCH c.items
           WHERE c.id = :cartId
           """)
    Optional<Cart> findCartWithItems(@Param("cartId") UUID cartId);
}