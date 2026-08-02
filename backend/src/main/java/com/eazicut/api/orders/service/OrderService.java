package com.eazicut.api.orders.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.eazicut.api.cart.entity.Cart;
import com.eazicut.api.cart.entity.CartItem;
import com.eazicut.api.cart.exception.InsufficientStockException;
import com.eazicut.api.cart.exception.ProductUnavailableException;
import com.eazicut.api.cart.exception.SizeUnavailableException;
import com.eazicut.api.cart.repository.CartRepository;
import com.eazicut.api.common.exception.ResourceNotFoundException;
import com.eazicut.api.orders.dto.CreateOrderRequest;
import com.eazicut.api.orders.dto.CreateShippingAddressRequest;
import com.eazicut.api.orders.dto.OrderResponse;
import com.eazicut.api.orders.entity.Order;
import com.eazicut.api.orders.entity.OrderItem;
import com.eazicut.api.orders.entity.OrderStatus;
import com.eazicut.api.orders.entity.ShippingAddress;
import com.eazicut.api.orders.exception.CartEmptyException;
import com.eazicut.api.orders.exception.MissingIdempotencyKeyException;
import com.eazicut.api.orders.exception.PriceMismatchException;
import com.eazicut.api.orders.exception.UnknownDeliveryMethodException;
import com.eazicut.api.orders.mapper.OrderMapper;
import com.eazicut.api.orders.repository.OrderRepository;
import com.eazicut.api.orders.service.DeliveryMethodCatalog.Method;
import com.eazicut.api.products.entity.Product;
import com.eazicut.api.products.entity.ProductStatus;
import com.eazicut.api.users.entity.User;

import lombok.RequiredArgsConstructor;

/**
 * Order application service.
 *
 * <p>Stage 1 shipped the read-only surface. Stage 2 adds the
 * order-creation flow — the single enforcement point for the
 * B005 pricing invariant.
 *
 * <p><strong>Ownership.</strong> Every entry point takes a
 * {@link User} — resolved from the authenticated principal in a
 * controller (Stage 3). Read methods scope queries by
 * {@code user.id}; the create flow reads the caller's cart via
 * {@code cartRepository.findByUserId}. No cart id or order id from
 * URL / body is ever trusted.
 *
 * <p><strong>Pricing invariant enforcement (createFromCart):</strong>
 * <ol>
 *   <li>Validate the {@code Idempotency-Key} header is present.</li>
 *   <li>Idempotent replay: if a stored order already exists for
 *       {@code (user, key)}, return it verbatim — never a second
 *       write.</li>
 *   <li>Load the caller's cart; refuse if empty.</li>
 *   <li>Validate every line against the live product row: status
 *       {@code ACTIVE}, size still offered, stock ≥ quantity.</li>
 *   <li>Recompute {@code subtotal} from live product prices — NEVER
 *       from the cart snapshot.</li>
 *   <li>Look up shipping cost from
 *       {@link DeliveryMethodCatalog}.</li>
 *   <li>Compare {@code expectedTotal} with the recomputed total. If
 *       they diverge → refuse with {@link PriceMismatchException}.</li>
 *   <li>Persist {@link Order} + {@link OrderItem}s inside one
 *       transaction using live prices as immutable
 *       {@code unitPrice}. Clear the cart on the same commit.</li>
 * </ol>
 *
 * <p>Concurrent duplicate POSTs with the same key race on
 * {@code ux_order_idempotency_user}; the loser's
 * {@code DataIntegrityViolationException} is mapped to 409 by the
 * global handler, and a client retry hits the idempotency probe
 * path — returning the winner's order. Net effect: at most one
 * order per (user, key).
 */
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final CartRepository cartRepository;
    private final DeliveryMethodCatalog deliveryCatalog;
    private final OrderNumberGenerator referenceGenerator;

    // ------------------------------------------------------------------
    // Customer-facing reads (Stage 1)
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public OrderResponse findByIdForUser(User user, UUID orderId) {
        Order order = orderRepository.findByIdAndUserId(orderId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));
        return orderMapper.toResponse(order);
    }

    @Transactional(readOnly = true)
    public OrderResponse findByReferenceForUser(User user, String reference) {
        Order order = orderRepository.findByReferenceAndUserId(reference, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Order", reference));
        return orderMapper.toResponse(order);
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> findForUser(User user, Pageable pageable) {
        return orderRepository.findByUserId(user.getId(), pageable)
                .map(orderMapper::toResponse);
    }

    // ------------------------------------------------------------------
    // Admin-facing reads (Stage 1)
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public OrderResponse adminFindById(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));
        return orderMapper.toResponse(order);
    }

    @Transactional(readOnly = true)
    public OrderResponse adminFindByReference(String reference) {
        Order order = orderRepository.findByReference(reference)
                .orElseThrow(() -> new ResourceNotFoundException("Order", reference));
        return orderMapper.toResponse(order);
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> adminFindAll(Pageable pageable) {
        return orderRepository.findAll(pageable).map(orderMapper::toResponse);
    }

    // ------------------------------------------------------------------
    // Create (Stage 2) — the pricing enforcement point
    // ------------------------------------------------------------------

    /**
     * Convert the caller's cart into a new {@link Order} in
     * {@link OrderStatus#PENDING_PAYMENT}. See class Javadoc for the
     * validation + pricing enforcement order.
     *
     * @param user             authenticated principal
     * @param request          delivery method + address + expectedTotal
     * @param idempotencyKey   value of the {@code Idempotency-Key}
     *                         header — {@link MissingIdempotencyKeyException}
     *                         if null / blank
     */
    @Transactional
    public OrderResponse createFromCart(User user, CreateOrderRequest request, String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new MissingIdempotencyKeyException();
        }
        String key = idempotencyKey.trim();

        // Idempotent replay: same (user, key) → return the stored order.
        var replay = orderRepository.findByUserIdAndIdempotencyKey(user.getId(), key);
        if (replay.isPresent()) {
            return orderMapper.toResponse(replay.get());
        }

        // Cart existence + non-empty
        Cart cart = cartRepository.findByUserId(user.getId())
                .orElseThrow(CartEmptyException::new);
        if (cart.getItems().isEmpty()) {
            throw new CartEmptyException();
        }

        // Delivery method lookup — 400 if fabricated / retired
        Method delivery = deliveryCatalog.find(request.deliveryMethodId())
                .orElseThrow(() -> new UnknownDeliveryMethodException(request.deliveryMethodId()));

        // Per-line validation against LIVE product state; also compute the
        // authoritative subtotal from LIVE prices.
        BigDecimal computedSubtotal = BigDecimal.ZERO;
        for (CartItem line : cart.getItems()) {
            Product product = line.getProduct();
            if (product == null || product.getStatus() != ProductStatus.ACTIVE) {
                throw new ProductUnavailableException(line.getSnapshotSlug());
            }
            if (product.getAvailableSizes() == null
                    || !product.getAvailableSizes().contains(line.getSize())) {
                throw new SizeUnavailableException(product.getSlug(), line.getSize());
            }
            if (product.getStockQuantity() < line.getQuantity()) {
                throw new InsufficientStockException(
                        product.getSlug(), line.getQuantity(), product.getStockQuantity());
            }
            computedSubtotal = computedSubtotal.add(
                    product.getPrice().multiply(BigDecimal.valueOf(line.getQuantity())));
        }

        BigDecimal computedTotal = computedSubtotal.add(delivery.cost());

        // Pricing invariant — refuse if the customer's expected total
        // disagrees with the server's live-price recompute.
        if (computedTotal.compareTo(request.expectedTotal()) != 0) {
            throw new PriceMismatchException(request.expectedTotal(), computedTotal);
        }

        // Compose + persist the order.
        Order order = new Order();
        order.setUser(user);
        order.setReference(referenceGenerator.generateUnique());
        order.setStatus(OrderStatus.PENDING_PAYMENT);
        order.setCurrency(cart.getCurrency());
        order.setSubtotal(computedSubtotal);
        order.setShippingCost(delivery.cost());
        order.setTotal(computedTotal);
        order.setDeliveryMethodId(delivery.id());
        order.setDeliveryMethodName(delivery.name());
        order.setShippingAddress(toShippingAddress(request.shippingAddress()));
        order.setIdempotencyKey(key);
        order.setPlacedAt(Instant.now());

        for (CartItem line : cart.getItems()) {
            Product product = line.getProduct();
            OrderItem item = new OrderItem();
            item.setOrder(order);
            item.setProduct(product);
            item.setProductSlug(product.getSlug());
            item.setProductName(product.getName());
            item.setProductImageUrl(line.getSnapshotImageUrl());
            item.setSize(line.getSize());
            item.setQuantity(line.getQuantity());
            item.setUnitPrice(product.getPrice());   // LIVE price, not snapshot
            item.setCurrency(product.getCurrency());
            item.setLineTotal(product.getPrice().multiply(BigDecimal.valueOf(line.getQuantity())));
            order.getItems().add(item);
        }

        Order saved = orderRepository.saveAndFlush(order);

        // Clear the cart on the same transaction — success means
        // conversion, not draft.
        cart.getItems().clear();
        cartRepository.saveAndFlush(cart);

        return orderMapper.toResponse(saved);
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private ShippingAddress toShippingAddress(CreateShippingAddressRequest req) {
        ShippingAddress addr = new ShippingAddress();
        addr.setFullName(req.fullName().trim());
        addr.setEmail(req.email().trim());
        addr.setPhone(req.phone().trim());
        addr.setAddressLine1(req.addressLine1().trim());
        addr.setAddressLine2(trimToNull(req.addressLine2()));
        addr.setCity(req.city().trim());
        addr.setRegion(trimToNull(req.region()));
        addr.setPostalCode(trimToNull(req.postalCode()));
        addr.setCountry(req.country().trim());
        return addr;
    }

    private static String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
