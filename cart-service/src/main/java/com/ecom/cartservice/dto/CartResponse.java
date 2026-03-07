package com.ecom.cartservice.dto;

import com.ecom.cartservice.model.CartStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CartResponse {

    private UUID cartId;
    private String userId;
    private String guestId;
    private CartStatus status;
    private BigDecimal totalAmount;
    private List<CartItemResponse> items;
}

