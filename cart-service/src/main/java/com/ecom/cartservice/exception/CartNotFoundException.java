package com.ecom.cartservice.exception;

public class CartNotFoundException extends BaseException{
    public CartNotFoundException(String cartId) {
        super("CART_NOT_FOUND", "Cart not found: " + cartId);
    }
}
