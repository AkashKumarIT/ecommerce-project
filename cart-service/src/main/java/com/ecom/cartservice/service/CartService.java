package com.ecom.cartservice.service;

import com.ecom.cartservice.client.InventoryServiceClient;
import com.ecom.cartservice.client.ProductServiceClient;
import com.ecom.cartservice.dto.*;
import com.ecom.cartservice.exception.BaseException;
import com.ecom.cartservice.mapper.CartMapper;
import com.ecom.cartservice.model.*;
import com.ecom.cartservice.repository.*;
import com.ecom.events.cart.CartCheckoutInitiatedEvent;
import com.ecom.events.order.OrderCancelledEvent;
import com.ecom.events.order.OrderConfirmedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class CartService {

    private final CartRepository cartRepository;
    private final IdempotencyRecordRepository idempotencyRepository;
    private final ProductServiceClient productClient;
    private final InventoryServiceClient inventoryClient;
    private final OutboxEventRepository outboxEventRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final CartMapper cartMapper;
    private final ObjectMapper objectMapper;
    private static final int MAX_RETRY = 3;

    @Transactional
    public CartResponse addItem(
            String userId,
            String guestId,
            AddToCartRequest request,
            String idempotencyKey,
            String token
    ) {

        int attempt = 0;

        while (attempt < MAX_RETRY) {

            try {

                return processAddItem(userId, guestId, request, idempotencyKey, token);

            } catch (ObjectOptimisticLockingFailureException ex) {

                attempt++;
                log.warn("Optimistic lock conflict. Retrying attempt={}", attempt);

                if (attempt >= MAX_RETRY) {
                    throw new BaseException("CONCURRENT_UPDATE",
                            "Cart is being updated. Please retry.");
                }
            }
        }

        throw new BaseException("UNKNOWN_ERROR", "Unexpected error");
    }

    private CartResponse processAddItem(
            String userId,
            String guestId,
            AddToCartRequest request,
            String idempotencyKey,
            String token
    ) {
        // 1️⃣ Idempotency Check
        Optional<IdempotencyRecord> existing =
                idempotencyRepository.findByIdempotencyKey(idempotencyKey);

        if (existing.isPresent()) {

            String existingHash = existing.get().getRequestHash();
            String currentHash = serialize(request);

            if (!existingHash.equals(currentHash)) {
                throw new BaseException("IDEMPOTENCY_CONFLICT",
                        "Same idempotency key used with different request");
            }
            log.info("Returning idempotent response for key={}", idempotencyKey);
            return deserialize(existing.get().getResponseBody());
        }

        // 2️⃣ Fetch Product (sync call)
        ProductResponse product =
                productClient.getProduct(request.getProductId(), token);

        if (product == null) {
            throw new BaseException("PRODUCT_NOT_FOUND",
                    "Product not found");
        }




        // 3️⃣ Load or Create Cart
        Cart cart = loadOrCreateCart(userId, guestId);

        if (cart.getStatus() != CartStatus.ACTIVE) {
            throw new BaseException("INVALID_CART_STATE",
                    "Cart cannot be modified after checkout");
        }

        int existingQty = cart.getItems().stream()
                .filter(i -> i.getProductId().equals(product.getId()))
                .map(CartItem::getQuantity)
                .findFirst()
                .orElse(0);

        int newTotalQty = existingQty + request.getQuantity();

        InventoryResponse stock =
                inventoryClient.getInventory(product.getSku());

        if (stock.getAvailableQty() < newTotalQty) {

            throw new BaseException(
                    "INSUFFICIENT_STOCK",
                    "Only " + stock.getAvailableQty() + " items available"
            );
        }

        // 4️⃣ Update Aggregate
        updateCartAggregate(cart, request, product);

        // 5️⃣ Recalculate Total
        recalculateTotal(cart);

        cart.setUpdatedAt(Instant.now());

        // 6️⃣ Save Cart (Optimistic Lock Safe)
        Cart saved = cartRepository.save(cart);

        // 7️⃣ Map Response
        CartResponse response = cartMapper.mapToCartResponse(saved);

        // 8️⃣ Save Idempotency Record
        IdempotencyRecord record = IdempotencyRecord.builder()
                .id(UUID.randomUUID())
                .idempotencyKey(idempotencyKey)
                .requestHash(serialize(request))
                .responseBody(serialize(response))
                .createdAt(Instant.now())
                .build();

        idempotencyRepository.save(record);

        // 9️⃣ Redis Write-Through
        updateRedis(saved, response);

        return response;
    }

    private Cart loadOrCreateCart(String userId, String guestId) {

        Optional<Cart> existing;

        if (userId != null && !userId.isBlank()) {
            existing = cartRepository
                    .findActiveUserCartWithItems(userId, CartStatus.ACTIVE);
        } else {
            existing = cartRepository
                    .findActiveGuestCartWithItems(guestId, CartStatus.ACTIVE);
        }

        if (existing.isPresent()) {
            return cartRepository
                    .findCartWithItems(existing.get().getId())
                    .orElseThrow();
        }

        Cart newCart = Cart.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .guestId(guestId)
                .status(CartStatus.ACTIVE)
                .totalAmount(BigDecimal.ZERO)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .items(new ArrayList<>())
                .build();

        return newCart;
    }

    private void updateCartAggregate(
            Cart cart,
            AddToCartRequest request,
            ProductResponse product
    ) {

        Optional<CartItem> existingItem =
                cart.getItems().stream()
                        .filter(i -> i.getProductId()
                                .equals(product.getId()))
                        .findFirst();

        if (existingItem.isPresent()) {

            CartItem item = existingItem.get();
            int newQty = item.getQuantity() + request.getQuantity();

            item.setQuantity(newQty);
            item.setSubtotal(
                    product.getPrice()
//                    BigDecimal.valueOf()
                            .multiply(BigDecimal.valueOf(newQty))
            );
            item.setUpdatedAt(Instant.now());

        } else {

            CartItem newItem = CartItem.builder()
                    .id(UUID.randomUUID())
                    .cart(cart)
                    .productId(product.getId())
                    .sku(product.getSku())
                    .productName(product.getName())
                    .productImage(product.getImageUrl())
                    .priceSnapshot(
//                            BigDecimal.valueOf(product.getPrice())
                            product.getPrice())
                    .quantity(request.getQuantity())
                    .subtotal(
//                            BigDecimal.valueOf(product.getPrice())
                            product.getPrice()
                                    .multiply(
                                            BigDecimal.valueOf(
                                                    request.getQuantity())))
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();

            cart.getItems().add(newItem);
        }
    }

    private void recalculateTotal(Cart cart) {

        BigDecimal total = cart.getItems().stream()
                .map(CartItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        cart.setTotalAmount(total);
    }

    private void updateRedis(Cart cart, CartResponse response) {

        String key = buildRedisKey(cart.getUserId(), cart.getGuestId());

        if (cart.getUserId() != null) {
            redisTemplate.opsForValue().set(key, response, Duration.ofDays(7));
        } else {
            redisTemplate.opsForValue()
                    .set(key, response, Duration.ofDays(7));
        }
    }

    private String serialize(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            log.error("Serialization failed", e);
            throw new BaseException("SERIALIZATION_ERROR",
                    "Failed to serialize response");
        }
    }
    private CartResponse deserialize(String json) {
        try {
            return objectMapper.readValue(json, CartResponse.class);
        } catch (Exception e) {
            log.error("Deserialization failed", e);
            throw new BaseException("DESERIALIZATION_ERROR",
                    "Failed to deserialize idempotent response");
        }
    }

    @Transactional(readOnly = true)
    public CartResponse getCart(String userId, String guestId) {

        String key = buildRedisKey(userId, guestId);

        // 1️⃣ Try Redis first
        Object cached = redisTemplate.opsForValue().get(key);

        if (cached != null) {
            log.info("Cart fetched from Redis for key={}", key);
            return objectMapper.convertValue(cached, CartResponse.class);
        }

        log.info("Cart not found in Redis. Loading from DB...");

        // 2️⃣ Load from DB
        Optional<Cart> cartOptional;

        if (userId != null && !userId.isBlank()) {
            cartOptional = cartRepository
                    .findActiveUserCartWithItems(userId, CartStatus.ACTIVE);
        } else {
            cartOptional = cartRepository
                    .findActiveGuestCartWithItems(guestId, CartStatus.ACTIVE);
        }

        if (cartOptional.isEmpty()) {
            // 🔥 Return empty cart instead of exception
            return CartResponse.builder()
                    .cartId(null)
                    .userId(userId)
                    .guestId(guestId)
                    .status(CartStatus.ACTIVE)
                    .totalAmount(BigDecimal.ZERO)
                    .items(Collections.emptyList())
                    .build();
        }

        Cart cart = cartRepository
                .findCartWithItems(cartOptional.get().getId())
                .orElseThrow();

        CartResponse response = cartMapper.mapToCartResponse(cart);

        // 3️⃣ Store in Redis (write-through)
        redisTemplate.opsForValue().set(key, response);

        return response;
    }

    private String buildRedisKey(String userId, String guestId) {

        if (userId != null && !userId.isBlank()) {
            return "cart:user:" + userId;
        } else {
            return "cart:guest:" + guestId;
        }
    }

    @Transactional
    public CartResponse updateQuantity(
            String userId,
            String guestId,
            UUID productId,
            UpdateQuantityRequest request,
            String idempotencyKey
    ) {

        int attempt = 0;

        while (attempt < MAX_RETRY) {

            try {

                return processUpdateQuantity(
                        userId,
                        guestId,
                        productId,
                        request,
                        idempotencyKey
                );

            } catch (ObjectOptimisticLockingFailureException ex) {

                attempt++;
                log.warn("Optimistic lock conflict during update. Retry={}", attempt);

                if (attempt >= MAX_RETRY) {
                    throw new BaseException("CONCURRENT_UPDATE",
                            "Cart is being updated. Please retry.");
                }
            }
        }

        throw new BaseException("UNKNOWN_ERROR", "Unexpected error");
    }


    private CartResponse processUpdateQuantity(
            String userId,
            String guestId,
            UUID productId,
            UpdateQuantityRequest request,
            String idempotencyKey
    ) {

        // 1️⃣ Idempotency Check
        Optional<IdempotencyRecord> existing =
                idempotencyRepository.findByIdempotencyKey(idempotencyKey);

        if (existing.isPresent()) {

            String existingHash = existing.get().getRequestHash();
            String currentHash = serialize(request);

            if (!existingHash.equals(currentHash)) {
                throw new BaseException("IDEMPOTENCY_CONFLICT",
                        "Same idempotency key used with different request");
            }

            return deserialize(existing.get().getResponseBody());
        }

        // 2️⃣ Load Cart
        Cart cart = loadActiveCart(userId, guestId);

        if (cart.getStatus() != CartStatus.ACTIVE) {
            throw new BaseException("INVALID_CART_STATE",
                    "Cart cannot be modified after checkout");
        }
        // 3️⃣ Find Item
        CartItem item = cart.getItems().stream()
                .filter(i -> i.getProductId().equals(productId))
                .findFirst()
                .orElseThrow(() ->
                        new BaseException("CART_ITEM_NOT_FOUND",
                                "Item not found in cart"));


        if (request.getQuantity() > 0) {

            InventoryResponse stock =
                    inventoryClient.getInventory(item.getSku());

            if (stock.getAvailableQty() < request.getQuantity()) {

                throw new BaseException(
                        "INSUFFICIENT_STOCK",
                        "Only " + stock.getAvailableQty() + " items available"
                );
            }
        }


        // 4️⃣ If quantity == 0 → remove item
        if (request.getQuantity() == 0) {

            cart.getItems().remove(item);

        } else {

            item.setQuantity(request.getQuantity());
            item.setSubtotal(
                    item.getPriceSnapshot()
                            .multiply(BigDecimal.valueOf(request.getQuantity()))
            );
            item.setUpdatedAt(Instant.now());
        }

        // 5️⃣ Recalculate total
        recalculateTotal(cart);

        cart.setUpdatedAt(Instant.now());

        Cart saved = cartRepository.save(cart);

        CartResponse response = cartMapper.mapToCartResponse(saved);

        // 6️⃣ Save idempotency
        IdempotencyRecord record = IdempotencyRecord.builder()
                .id(UUID.randomUUID())
                .idempotencyKey(idempotencyKey)
                .requestHash(serialize(request))
                .responseBody(serialize(response))
                .createdAt(Instant.now())
                .build();

        idempotencyRepository.save(record);

        // 7️⃣ Update Redis
        updateRedis(saved, response);

        return response;
    }

    private Cart loadActiveCart(String userId, String guestId) {

        Optional<Cart> cartOptional;

        if (userId != null) {
            cartOptional = cartRepository
                    .findActiveUserCartWithItems(userId, CartStatus.ACTIVE);
        } else {
            cartOptional = cartRepository
                    .findActiveGuestCartWithItems(guestId, CartStatus.ACTIVE);
        }

        if (cartOptional.isEmpty()) {
            throw new BaseException("CART_NOT_FOUND",
                    "No active cart found");
        }

        return cartRepository
                .findCartWithItems(cartOptional.get().getId())
                .orElseThrow();
    }


    @Transactional
    public CartResponse removeItem(
            String userId,
            String guestId,
            UUID productId,
            String idempotencyKey
    ) {

        int attempt = 0;

        while (attempt < MAX_RETRY) {

            try {

                return processRemoveItem(
                        userId,
                        guestId,
                        productId,
                        idempotencyKey
                );

            } catch (ObjectOptimisticLockingFailureException ex) {

                attempt++;
                log.warn("Optimistic lock conflict during remove. Retry={}", attempt);

                if (attempt >= MAX_RETRY) {
                    throw new BaseException("CONCURRENT_UPDATE",
                            "Cart is being updated. Please retry.");
                }
            }
        }

        throw new BaseException("UNKNOWN_ERROR", "Unexpected error");
    }

    private CartResponse processRemoveItem(
            String userId,
            String guestId,
            UUID productId,
            String idempotencyKey
    ) {

        // 1️⃣ Idempotency Check
        Optional<IdempotencyRecord> existing =
                idempotencyRepository.findByIdempotencyKey(idempotencyKey);

        String currentHash = "REMOVE:" + productId;

        if (existing.isPresent()) {

            if (!existing.get().getRequestHash().equals(currentHash)) {
                throw new BaseException("IDEMPOTENCY_CONFLICT",
                        "Same idempotency key used with different request");
            }

            return deserialize(existing.get().getResponseBody());
        }

        // 2️⃣ Load Cart
        Cart cart = loadActiveCart(userId, guestId);

        if (cart.getStatus() != CartStatus.ACTIVE) {
            throw new BaseException("INVALID_CART_STATE",
                    "Cart cannot be modified after checkout");
        }

        // 3️⃣ Find Item
        CartItem item = cart.getItems().stream()
                .filter(i -> i.getProductId().equals(productId))
                .findFirst()
                .orElseThrow(() ->
                        new BaseException("CART_ITEM_NOT_FOUND",
                                "Item not found in cart"));

        // 4️⃣ Remove Item
        cart.getItems().remove(item);

        // 5️⃣ Recalculate Total
        recalculateTotal(cart);

        cart.setUpdatedAt(Instant.now());

        Cart saved = cartRepository.save(cart);

        CartResponse response = cartMapper.mapToCartResponse(saved);

        // 6️⃣ Save Idempotency
        IdempotencyRecord record = IdempotencyRecord.builder()
                .id(UUID.randomUUID())
                .idempotencyKey(idempotencyKey)
                .requestHash("REMOVE:" + productId)
                .responseBody(serialize(response))
                .createdAt(Instant.now())
                .build();

        idempotencyRepository.save(record);

        // 7️⃣ Redis Write-Through
        updateRedis(saved, response);

        return response;
    }


    @Transactional
    public void checkout(String userId, String guestId, String idempotencyKey) {
        log.info("CHECKOUT START - userId={}, guestId={}", userId, guestId);
        // 1️⃣ Idempotency Check
        Optional<IdempotencyRecord> existing =
                idempotencyRepository.findByIdempotencyKey(idempotencyKey);

        if (existing.isPresent()) {

            if (!existing.get().getRequestHash().equals("CHECKOUT:" + userId)) {
                throw new BaseException("IDEMPOTENCY_CONFLICT",
                        "Same idempotency key used with different request");
            }

            log.info("Returning idempotent checkout response for key={}",
                    idempotencyKey);

            return;
        }

        if (userId == null) {
            throw new BaseException("AUTH_REQUIRED", "Login required");
        }

        // 🔥 Step 1: Merge guest cart if exists
        if (guestId != null) {
            mergeGuestCartIntoUserCart(userId, guestId);
        }

        log.info("Loading user cart after merge for userId={}", userId);

        // 🔥 Step 2: Load ONLY user cart
        Cart cart = cartRepository
                .findActiveUserCartWithItems(userId, CartStatus.ACTIVE)
                .flatMap(c -> cartRepository.findCartWithItems(c.getId()))
                .orElseThrow(() ->
                        new BaseException("CART_NOT_FOUND",
                                "No active cart found"));

        log.info("User cart items count after merge = {}", cart.getItems().size());

        if (cart.getItems().isEmpty()) {
            throw new BaseException("CART_EMPTY",
                    "Cannot checkout empty cart");
        }

        if (cart.getStatus() != CartStatus.ACTIVE) {
            throw new BaseException("INVALID_CART_STATE",
                    "Cart already processed");
        }

        cart.setStatus(CartStatus.CHECKOUT_INITIATED);
        cart.setUpdatedAt(Instant.now());

        cartRepository.save(cart);

        createCheckoutEvent(cart);

        updateRedis(cart, cartMapper.mapToCartResponse(cart));

        IdempotencyRecord record = IdempotencyRecord.builder()
                .id(UUID.randomUUID())
                .idempotencyKey(idempotencyKey)
                .requestHash("CHECKOUT:" + userId)
                .responseBody("SUCCESS")
                .createdAt(Instant.now())
                .build();

        idempotencyRepository.save(record);
    }

    private void mergeGuestCartIntoUserCart(String userId, String guestId) {
        log.info("MERGE START - userId={}, guestId={}", userId, guestId);

        Optional<Cart> guestCartOpt =
                cartRepository.findActiveGuestCartWithItems(guestId, CartStatus.ACTIVE);

        log.info("Guest cart found? {}", guestCartOpt.isPresent());

        if (guestCartOpt.isEmpty()) {
            return; // nothing to merge
        }

        Cart guestCart = cartRepository
                .findCartWithItems(guestCartOpt.get().getId())
                .orElseThrow();

        log.info("Guest cart id = {}", guestCartOpt.map(Cart::getId).orElse(null));

        Cart userCart = cartRepository
                .findActiveUserCartWithItems(userId, CartStatus.ACTIVE)
                .orElseGet(() -> createNewUserCart(userId));

        for (CartItem guestItem : guestCart.getItems()) {

            Optional<CartItem> existing =
                    userCart.getItems().stream()
                            .filter(i -> i.getProductId()
                                    .equals(guestItem.getProductId()))
                            .findFirst();

            if (existing.isPresent()) {
                CartItem item = existing.get();
                InventoryResponse stock =
                        inventoryClient.getInventory(guestItem.getSku());

                int mergedQty = item.getQuantity() + guestItem.getQuantity();

                if (stock.getAvailableQty() < mergedQty) {
                    throw new BaseException("INSUFFICIENT_STOCK",
                            "Stock not sufficient during cart merge");
                }
                item.setQuantity(item.getQuantity() + guestItem.getQuantity());
                item.setSubtotal(
                        item.getPriceSnapshot()
                                .multiply(BigDecimal.valueOf(item.getQuantity()))
                );
            } else {
                guestItem.setCart(userCart);
                userCart.getItems().add(guestItem);
            }
        }

        log.info("After merge - userCart items count={}", userCart.getItems().size());

        recalculateTotal(userCart);

        cartRepository.save(userCart);

        log.info("Deleting guest cart id={}", guestCart.getId());
        // delete guest cart
        cartRepository.delete(guestCart);

        redisTemplate.delete(buildRedisKey(null, guestId));
    }

    private void createCheckoutEvent(Cart cart) {

        CartCheckoutInitiatedEvent payload = new CartCheckoutInitiatedEvent(
                cart.getId(),
                cart.getUserId(),
                cart.getTotalAmount(),
                cart.getItems().stream()
                        .map(item -> new CartCheckoutInitiatedEvent.Item(
                                item.getSku(),
                                item.getQuantity(),
                                item.getPriceSnapshot()
                        ))
                        .toList()
        );

        OutboxEvent event = OutboxEvent.builder()
                .id(UUID.randomUUID())
                .aggregateId(cart.getId())
                .aggregateType("CART")
                .eventType("CART_CHECKOUT_INITIATED")
                .payload(serialize(payload))
                .status("NEW")
                .createdAt(Instant.now())
                .build();

        outboxEventRepository.save(event);
    }

    private Cart createNewUserCart(String userId) {

        Cart newCart = Cart.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .guestId(null)
                .status(CartStatus.ACTIVE)
                .totalAmount(BigDecimal.ZERO)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .items(new ArrayList<>())
                .build();

        return cartRepository.save(newCart);
    }

    @Transactional
    public void handleOrderConfirmed(OrderConfirmedEvent event) {

        Cart cart = cartRepository
                .findById(event.getCartId())
                .orElseThrow();

        if (cart.getStatus() != CartStatus.CHECKOUT_INITIATED) {
            return;
        }

        cart.setStatus(CartStatus.COMPLETED);
        cart.setUpdatedAt(Instant.now());
        cartRepository.save(cart);

        Cart newCart = createNewUserCart(cart.getUserId());

        updateRedis(newCart, cartMapper.mapToCartResponse(newCart));
    }

    @Transactional
    public void handleOrderCancelled(OrderCancelledEvent event) {

        Cart cart = cartRepository
                .findById(event.getCartId())
                .orElseThrow();

        if (cart.getStatus() != CartStatus.CHECKOUT_INITIATED) {
            return;
        }

        cart.setStatus(CartStatus.ACTIVE);
        cart.setUpdatedAt(Instant.now());

        cartRepository.save(cart);

        updateRedis(cart, cartMapper.mapToCartResponse(cart));
    }
}
