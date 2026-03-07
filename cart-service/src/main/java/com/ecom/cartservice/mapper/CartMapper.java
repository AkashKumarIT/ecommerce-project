package com.ecom.cartservice.mapper;

import com.ecom.cartservice.dto.CartItemResponse;
import com.ecom.cartservice.model.Cart;
import com.ecom.cartservice.model.CartItem;
import org.springframework.stereotype.Component;
import com.ecom.cartservice.dto.CartResponse;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class CartMapper {
    public CartResponse mapToCartResponse(Cart cart) {

        CartResponse response = new CartResponse();

        response.setCartId(cart.getId());
        response.setUserId(cart.getUserId());
        response.setGuestId(cart.getGuestId());
        response.setStatus(cart.getStatus());
        response.setTotalAmount(cart.getTotalAmount());

        // 🔥 Proper conversion
        List<CartItemResponse> itemResponses =
                cart.getItems()
                        .stream()
                        .map(this::mapToCartItemResponse)
                        .collect(Collectors.toList());

        response.setItems(itemResponses);

        return response;
    }

    private CartItemResponse mapToCartItemResponse(CartItem item) {

        return CartItemResponse.builder()
                .productId(item.getProductId())
                .sku(item.getSku())
                .productName(item.getProductName())
                .productImage(item.getProductImage())
                .price(item.getPriceSnapshot())
                .quantity(item.getQuantity())
                .subtotal(item.getSubtotal())
                .build();
    }
}
