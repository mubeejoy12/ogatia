package com.eazicut.api.cart.exception;

import java.util.UUID;

import com.eazicut.api.common.exception.ResourceNotFoundException;

/**
 * Thrown when a cart operation targets an item id that either doesn't
 * exist or belongs to a different user's cart.
 *
 * <p>Deliberately does NOT distinguish "wrong cart" from "no such id"
 * — both map to 404 with the same shape. Leaking "exists but not
 * yours" would let a caller enumerate other users' item ids.
 */
public class CartLineNotFoundException extends ResourceNotFoundException {

    public CartLineNotFoundException(UUID itemId) {
        super("CartItem", itemId);
    }
}
