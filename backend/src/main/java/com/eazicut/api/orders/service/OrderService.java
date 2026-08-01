package com.eazicut.api.orders.service;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.eazicut.api.common.exception.ResourceNotFoundException;
import com.eazicut.api.orders.dto.OrderResponse;
import com.eazicut.api.orders.entity.Order;
import com.eazicut.api.orders.mapper.OrderMapper;
import com.eazicut.api.orders.repository.OrderRepository;
import com.eazicut.api.users.entity.User;

import lombok.RequiredArgsConstructor;

/**
 * Order application service — Stage 1 read-only surface.
 *
 * <p>Stage 1 exposes the customer-facing read methods
 * ({@link #findByIdForUser}, {@link #findByReferenceForUser},
 * {@link #findForUser}) plus the admin bypass reads
 * ({@link #adminFindById}, {@link #adminFindByReference},
 * {@link #adminFindAll}) that Stage 4's status-transition endpoint
 * will need.
 *
 * <p>Order creation (Stage 2), status transitions (Stage 4), and any
 * mutating path deliberately do not live here yet.
 *
 * <p><strong>Ownership.</strong> Customer-facing lookups always
 * scope by {@code user.id} at the repository layer — a fabricated or
 * enumerated order id resolves to a 404, indistinguishable from a
 * real typo. Same pattern B005 established for
 * {@code CartItemRepository.findByIdAndCartId}. Admin-facing methods
 * are prefixed {@code adminX} so a naming slip (calling the wrong
 * variant in a customer path) is obvious at the call site.
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;

    // ------------------------------------------------------------------
    // Customer-facing (ownership-safe)
    // ------------------------------------------------------------------

    /**
     * Look up one of the caller's orders by id. Cross-user access,
     * unknown id, and deleted user all resolve to the same 404 shape
     * so the id space cannot be enumerated.
     */
    public OrderResponse findByIdForUser(User user, UUID orderId) {
        Order order = orderRepository.findByIdAndUserId(orderId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));
        return orderMapper.toResponse(order);
    }

    /**
     * Look up one of the caller's orders by human-facing reference.
     * Used by the confirmation page after the customer completes
     * checkout.
     */
    public OrderResponse findByReferenceForUser(User user, String reference) {
        Order order = orderRepository.findByReferenceAndUserId(reference, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Order", reference));
        return orderMapper.toResponse(order);
    }

    /**
     * Paged history of the caller's orders.
     */
    public Page<OrderResponse> findForUser(User user, Pageable pageable) {
        return orderRepository.findByUserId(user.getId(), pageable)
                .map(orderMapper::toResponse);
    }

    // ------------------------------------------------------------------
    // Admin-facing (bypasses ownership filter — call sites are gated
    // by @PreAuthorize("hasRole('ADMIN')") on the controller in later
    // stages)
    // ------------------------------------------------------------------

    public OrderResponse adminFindById(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));
        return orderMapper.toResponse(order);
    }

    public OrderResponse adminFindByReference(String reference) {
        Order order = orderRepository.findByReference(reference)
                .orElseThrow(() -> new ResourceNotFoundException("Order", reference));
        return orderMapper.toResponse(order);
    }

    public Page<OrderResponse> adminFindAll(Pageable pageable) {
        return orderRepository.findAll(pageable).map(orderMapper::toResponse);
    }
}
