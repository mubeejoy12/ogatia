package com.eazicut.api.cart.controller;

import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.eazicut.api.auth.exception.InvalidCredentialsException;
import com.eazicut.api.cart.dto.AddCartItemRequest;
import com.eazicut.api.cart.dto.CartResponse;
import com.eazicut.api.cart.dto.MergeCartRequest;
import com.eazicut.api.cart.dto.UpdateCartItemRequest;
import com.eazicut.api.cart.service.CartService;
import com.eazicut.api.common.dto.ApiResponse;
import com.eazicut.api.users.entity.User;
import com.eazicut.api.users.repository.UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * REST controller for the customer cart.
 *
 * <p>Base path {@code /api/v1/cart}. Every endpoint requires an
 * authenticated user — no allowlist entries in
 * {@code SecurityConfig}. The filter chain's
 * {@code .anyRequest().authenticated()} default (B004 Stage 3)
 * protects them automatically.
 *
 * <p><strong>Ownership.</strong> Every method extracts the caller's
 * email from the JWT principal, resolves the {@code User}, and hands
 * it to the {@code CartService}. No cart id appears in any URL and
 * the service does not accept one — IDOR is impossible by design.
 *
 * <p><strong>Every mutating response returns the full updated
 * {@link CartResponse}</strong> so the frontend replaces its state
 * atomically without local reconciliation. Matches the pattern the
 * frontend already uses for the localStorage cart (reducer returns
 * the whole state after every action).
 */
@RestController
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;
    private final UserRepository userRepository;

    @GetMapping
    public ApiResponse<CartResponse> getCart(@AuthenticationPrincipal String email) {
        User user = resolveUser(email);
        return ApiResponse.of(cartService.readForUser(user));
    }

    @PostMapping("/items")
    public ApiResponse<CartResponse> addItem(
            @AuthenticationPrincipal String email,
            @Valid @RequestBody AddCartItemRequest request
    ) {
        User user = resolveUser(email);
        return ApiResponse.of(cartService.add(user, request));
    }

    @PatchMapping("/items/{itemId}")
    public ApiResponse<CartResponse> updateItem(
            @AuthenticationPrincipal String email,
            @PathVariable UUID itemId,
            @Valid @RequestBody UpdateCartItemRequest request
    ) {
        User user = resolveUser(email);
        return ApiResponse.of(cartService.setQuantity(user, itemId, request));
    }

    @DeleteMapping("/items/{itemId}")
    public ApiResponse<CartResponse> removeItem(
            @AuthenticationPrincipal String email,
            @PathVariable UUID itemId
    ) {
        User user = resolveUser(email);
        return ApiResponse.of(cartService.remove(user, itemId));
    }

    @DeleteMapping
    public ApiResponse<CartResponse> clearCart(@AuthenticationPrincipal String email) {
        User user = resolveUser(email);
        return ApiResponse.of(cartService.clear(user));
    }

    /**
     * One-shot merge of a guest (localStorage) cart into the caller's
     * server-side cart. Called by the frontend immediately after
     * login. Never rejects the whole payload for a bad line — every
     * skipped guest line becomes an entry in
     * {@code CartResponse.issues[]}.
     */
    @PostMapping("/merge")
    public ApiResponse<CartResponse> mergeGuestCart(
            @AuthenticationPrincipal String email,
            @Valid @RequestBody MergeCartRequest request
    ) {
        User user = resolveUser(email);
        return ApiResponse.of(cartService.merge(user, request));
    }

    // ---------------------------------------------------------------------
    // Internals
    // ---------------------------------------------------------------------

    /**
     * Resolve the caller's email to a live {@link User}. Missing /
     * disabled / deleted mid-session → 401 (matches
     * {@code AuthService.me} semantics from B004 Stage 6).
     */
    private User resolveUser(String email) {
        if (email == null || email.isBlank()) {
            throw new InvalidCredentialsException();
        }
        return userRepository.findByEmailLower(email.trim().toLowerCase())
                .filter(User::isEnabled)
                .orElseThrow(InvalidCredentialsException::new);
    }
}
