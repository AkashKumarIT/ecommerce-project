package com.ecom.cartservice.controller;

import com.ecom.cartservice.dto.*;
import com.ecom.cartservice.exception.BaseException;
import com.ecom.cartservice.service.CartService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @PostMapping("/items")
    public ResponseEntity<ApiResponse<CartResponse>> addItem(
//            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(value = "Idempotency-Key") String idempotencyKey,
//            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @CookieValue(value = "GUEST_ID", required = false) String guestId,
            HttpServletResponse response,
            @Valid @RequestBody AddToCartRequest request
    ) {
        String userId = null;
        if (jwt != null) {
            userId = jwt.getSubject();
        }
        if (userId == null) {

            if (guestId == null) {
                guestId = UUID.randomUUID().toString();

                Cookie cookie = new Cookie("GUEST_ID", guestId);
                cookie.setHttpOnly(true);
                cookie.setPath("/");
                cookie.setMaxAge(60 * 60 * 24 * 7);

                response.addCookie(cookie);
            }
        } else {
            guestId = null;
        }

//        String token = null;
//
//        if (authHeader != null && authHeader.startsWith("Bearer ")) {
//            token = authHeader.substring(7);
//        }

        String token = jwt != null ? jwt.getTokenValue() : null;

        CartResponse result = cartService.addItem(
                userId,
                guestId,
                request,
                idempotencyKey,
                token
        );

        return ResponseEntity.ok(
                ApiResponse.<CartResponse>builder()
                        .success(true)
                        .data(result)
                        .build()
        );
    }

    @GetMapping
    public ResponseEntity<ApiResponse<CartResponse>> getCart(
//            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @AuthenticationPrincipal Jwt jwt,
            @CookieValue(value = "GUEST_ID", required = false) String guestId,
            HttpServletResponse response
    ) {
        String userId = null;
        if (jwt != null) {
            userId = jwt.getSubject();
        }

        if (userId == null) {

            if (guestId == null) {
                guestId = UUID.randomUUID().toString();

                Cookie cookie = new Cookie("GUEST_ID", guestId);
                cookie.setHttpOnly(true);
                cookie.setPath("/");
                cookie.setMaxAge(60 * 60 * 24 * 7); // 7 days

                response.addCookie(cookie);
            }
        } else {
            guestId = null;
        }

        CartResponse cart = cartService.getCart(userId, guestId);

        return ResponseEntity.ok(
                ApiResponse.<CartResponse>builder()
                        .success(true)
                        .data(cart)
                        .build()
        );
    }

    @PutMapping("/items/{productId}")
    public ResponseEntity<ApiResponse<CartResponse>> updateQuantity(
            @PathVariable UUID productId,
//            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @CookieValue(value = "GUEST_ID", required = false) String guestId,
            HttpServletResponse response,
            @Valid @RequestBody UpdateQuantityRequest request
    ) {
        String userId = null;
        if (jwt != null) {
            userId = jwt.getSubject();
        }

        if (userId == null && guestId == null) {
            throw new BaseException("GUEST_NOT_FOUND",
                    "Guest identifier missing");
        }

        CartResponse result = cartService.updateQuantity(
                userId,
                guestId,
                productId,
                request,
                idempotencyKey
        );

        return ResponseEntity.ok(
                ApiResponse.<CartResponse>builder()
                        .success(true)
                        .data(result)
                        .build()
        );
    }

    @DeleteMapping("/items/{productId}")
    public ResponseEntity<ApiResponse<CartResponse>> removeItem(
            @PathVariable UUID productId,
//            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @CookieValue(value = "GUEST_ID", required = false) String guestId
    ) {
        String userId = null;
        if (jwt != null) {
            userId = jwt.getSubject();
        }

        if (userId == null && guestId == null) {
            throw new BaseException("GUEST_NOT_FOUND",
                    "Guest identifier missing");
        }

        CartResponse result = cartService.removeItem(
                userId,
                guestId,
                productId,
                idempotencyKey
        );

        return ResponseEntity.ok(
                ApiResponse.<CartResponse>builder()
                        .success(true)
                        .data(result)
                        .build()
        );
    }

    @PostMapping("/checkout")
    public ResponseEntity<ApiResponse<String>> checkout(
//            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @CookieValue(value = "GUEST_ID", required = false) String guestId
    ) {
        if (jwt == null) {
            throw new BaseException("AUTH_REQUIRED", "Login required for checkout");
        }

        String userId = jwt.getSubject();

        if (userId == null) {
            throw new BaseException("AUTH_REQUIRED", "Login required for checkout");
        }

        cartService.checkout(userId, guestId,idempotencyKey);

        return ResponseEntity.ok(
                ApiResponse.<String>builder()
                        .success(true)
                        .data("Checkout initiated")
                        .build()
        );
    }

}
