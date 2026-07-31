package com.eazicut.api.cart.service;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.eazicut.api.cart.dto.CartItemResponse;
import com.eazicut.api.cart.dto.CartResponse;
import com.eazicut.api.cart.entity.Cart;
import com.eazicut.api.cart.mapper.CartMapper;
import com.eazicut.api.cart.repository.CartRepository;
import com.eazicut.api.users.entity.User;

import lombok.RequiredArgsConstructor;

/**
 * Cart application service — Stage 1 read-only surface.
 *
 * <p>Stage 1 exposes two operations:
 *
 * <ul>
 *   <li>{@link #getOrCreate} — lazily materialise the caller's cart.
 *       A user has exactly one cart, enforced by {@code ux_cart_user}.
 *       The first call from any user creates the row; subsequent calls
 *       return it.</li>
 *   <li>{@link #toResponse} — map a persisted cart into the wire
 *       {@link CartResponse}, including a subtotal computed from the
 *       snapshot price × quantity across all lines. See {@code CartItem}
 *       Javadoc: this subtotal is historical/display only — the
 *       Order pipeline decides the actual charged amount.</li>
 * </ul>
 *
 * <p>Later stages add mutations (add/patch/remove/clear/merge) and
 * populate {@link CartResponse#issues()} with read-time observations.
 * Both live here on the same façade.
 *
 * <p><strong>Ownership.</strong> The service NEVER accepts a cart id
 * from a caller. Every entry point takes a {@link User} — resolved
 * from the authenticated principal in the controller — and derives
 * the cart via {@link CartRepository#findByUserId}. That closes IDOR
 * at the design level: there is no cart-id in any URL.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class CartService {

    /**
     * Cart currency at launch. Multi-currency support means moving
     * this off the cart and onto the line — deferred until we actually
     * quote more than NGN.
     */
    private static final String DEFAULT_CURRENCY = "NGN";

    private final CartRepository cartRepository;
    private final CartMapper cartMapper;

    /**
     * Return the caller's cart, creating an empty one on first access.
     *
     * <p>Concurrent-first-access race: two requests from the same
     * fresh account can both miss the SELECT and race the INSERT. The
     * {@code ux_cart_user} unique index is the backstop — the loser's
     * INSERT throws {@code DataIntegrityViolationException} which the
     * GlobalExceptionHandler surfaces as 409. Caller can retry;
     * subsequent calls hit the row that survived. Acceptable for
     * launch — a first-add on a new account is a rare, low-frequency
     * event.
     */
    public Cart getOrCreate(User user) {
        return cartRepository.findByUserId(user.getId())
                .orElseGet(() -> createEmpty(user));
    }

    /**
     * Read-only projection into the wire shape.
     *
     * <p>The subtotal is the sum of {@code snapshotPrice × quantity}
     * across every line — historical value. The charged amount is
     * decided at Order time (see {@code CartItem} Javadoc).
     */
    @Transactional(readOnly = true)
    public CartResponse toResponse(Cart cart) {
        List<CartItemResponse> items = cart.getItems().stream()
                .map(cartMapper::toItemResponse)
                .toList();

        BigDecimal subtotal = cart.getItems().stream()
                .map(item -> item.getSnapshotPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int itemCount = cart.getItems().stream().mapToInt(item -> item.getQuantity()).sum();

        return new CartResponse(
                cart.getId(),
                cart.getCurrency(),
                items,
                subtotal,
                itemCount,
                Collections.emptyList(),   // populated in Stage 3
                cart.getUpdatedAt()
        );
    }

    // ---------------------------------------------------------------------
    // Internals
    // ---------------------------------------------------------------------

    private Cart createEmpty(User user) {
        Cart cart = new Cart();
        cart.setUser(user);
        cart.setCurrency(DEFAULT_CURRENCY);
        return cartRepository.save(cart);
    }
}
