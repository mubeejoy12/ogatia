package com.eazicut.api.orders.controller;

import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.eazicut.api.common.dto.ApiResponse;
import com.eazicut.api.common.dto.PagedResponse;
import com.eazicut.api.orders.dto.OrderResponse;
import com.eazicut.api.orders.dto.UpdateOrderStatusRequest;
import com.eazicut.api.orders.service.OrderService;

import lombok.RequiredArgsConstructor;

/**
 * Admin surface for orders. Every method is
 * {@code @PreAuthorize("hasRole('ADMIN')")} — a CUSTOMER with a
 * valid Bearer token gets 403, an anonymous caller gets 401
 * (per the B004 SecurityConfig posture).
 *
 * <p>Deliberately separated from {@link OrderController} so the
 * customer-facing surface stays cleanly free of admin concerns and
 * the admin URLs have a distinct base path
 * ({@code /admin/orders}) — a natural boundary for a later
 * admin-app deploy on its own subdomain if needed.
 *
 * <p><strong>Bypass of ownership.</strong> Admin reads use
 * {@code adminFindById} / {@code adminFindByReference} on the
 * service, which don't scope by user — an admin needs to see any
 * order for support and fulfilment.
 *
 * <p><strong>Status transitions.</strong> {@code PATCH
 * /admin/orders/{id}/status} moves an order through the lifecycle
 * defined in {@code OrderStatusTransitions}. Illegal transitions
 * (e.g. DELIVERED → SHIPPED) → 409
 * {@code invalid_status_transition}.
 */
@RestController
@RequestMapping("/admin/orders")
@RequiredArgsConstructor
public class AdminOrderController {

    private final OrderService orderService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public PagedResponse<OrderResponse> list(
            @PageableDefault(size = 20, sort = "placedAt", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {
        return PagedResponse.from(orderService.adminFindAll(pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<OrderResponse> getById(@PathVariable UUID id) {
        return ApiResponse.of(orderService.adminFindById(id));
    }

    @GetMapping("/reference/{reference}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<OrderResponse> getByReference(@PathVariable String reference) {
        return ApiResponse.of(orderService.adminFindByReference(reference));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<OrderResponse> updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateOrderStatusRequest request
    ) {
        return ApiResponse.of(orderService.adminUpdateStatus(id, request.status()));
    }
}
