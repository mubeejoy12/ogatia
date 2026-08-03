package com.eazicut.api.orders.controller;

import java.net.URI;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.eazicut.api.auth.exception.InvalidCredentialsException;
import com.eazicut.api.common.dto.ApiResponse;
import com.eazicut.api.common.dto.PagedResponse;
import com.eazicut.api.orders.dto.CreateOrderRequest;
import com.eazicut.api.orders.dto.OrderResponse;
import com.eazicut.api.orders.service.OrderService;
import com.eazicut.api.users.entity.User;
import com.eazicut.api.users.repository.UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * REST controller for customer-facing order operations.
 *
 * <p>Base path {@code /api/v1/orders}. Every endpoint requires an
 * authenticated user — no allowlist entries in {@code SecurityConfig}.
 * The filter chain's {@code .anyRequest().authenticated()} default
 * (B004 Stage 3) protects them automatically.
 *
 * <p><strong>Ownership.</strong> Every method resolves the caller's
 * {@link User} from the JWT principal and hands it to
 * {@link OrderService}. The service scopes queries by
 * {@code user.id} — a fabricated / enumerated order id or reference
 * belonging to another user resolves to 404, indistinguishable from
 * a real typo. Same closure pattern B005 used for {@code /cart}.
 *
 * <p><strong>Idempotency-Key.</strong> {@code POST /orders} reads the
 * header as {@code required = false} so the service (not Spring)
 * throws the domain {@link com.eazicut.api.orders.exception.MissingIdempotencyKeyException}
 * with a shaped 400 body. Setting {@code required = true} would trip
 * Spring's generic {@code MissingRequestHeaderException} — an
 * anonymous 400 the customer can't act on.
 *
 * <p>Admin endpoints and status-transition PATCH land in Stage 4.
 */
@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final UserRepository userRepository;

    /**
     * Convert the caller's cart into a new {@code PENDING_PAYMENT}
     * order. Returns 201 with a {@code Location} header pointing at
     * the just-created order's detail URL.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<OrderResponse>> create(
            @AuthenticationPrincipal String email,
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody CreateOrderRequest request
    ) {
        User user = resolveUser(email);
        OrderResponse created = orderService.createFromCart(user, request, idempotencyKey);

        URI location = ServletUriComponentsBuilder.fromCurrentRequestUri()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(ApiResponse.of(created));
    }

    /**
     * List the caller's orders. Default sort {@code placedAt DESC} —
     * newest first, matching customer expectation of an account order
     * history.
     */
    @GetMapping
    public PagedResponse<OrderResponse> listMine(
            @AuthenticationPrincipal String email,
            @PageableDefault(size = 20, sort = "placedAt", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {
        User user = resolveUser(email);
        return PagedResponse.from(orderService.findForUser(user, pageable));
    }

    /**
     * Fetch a single order by id. 404 if the id doesn't exist OR if
     * it belongs to a different customer (same shape either way —
     * enumeration is closed at the service layer).
     */
    @GetMapping("/{id}")
    public ApiResponse<OrderResponse> getById(
            @AuthenticationPrincipal String email,
            @PathVariable UUID id
    ) {
        User user = resolveUser(email);
        return ApiResponse.of(orderService.findByIdForUser(user, id));
    }

    /**
     * Fetch a single order by its human-facing reference
     * ({@code EAZI-<epoch>-<hex4>}). Same ownership scoping as
     * {@link #getById}.
     */
    @GetMapping("/reference/{reference}")
    public ApiResponse<OrderResponse> getByReference(
            @AuthenticationPrincipal String email,
            @PathVariable String reference
    ) {
        User user = resolveUser(email);
        return ApiResponse.of(orderService.findByReferenceForUser(user, reference));
    }

    // ------------------------------------------------------------------
    // Internals
    // ------------------------------------------------------------------

    /**
     * Resolve the caller's email to a live {@link User}. Missing /
     * disabled / deleted mid-session → 401 (matches
     * {@code CartController.resolveUser} and {@code AuthService.me}).
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
