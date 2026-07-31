package com.eazicut.api.cart.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.eazicut.api.cart.dto.CartIssueResponse;
import com.eazicut.api.cart.entity.Cart;
import com.eazicut.api.cart.entity.CartItem;
import com.eazicut.api.products.entity.Product;
import com.eazicut.api.products.entity.ProductStatus;

/**
 * Read-time inspector — produces a list of {@link CartIssueResponse}s
 * describing every observed mismatch between a cart's frozen snapshots
 * and the live product state.
 *
 * <p>Called from {@code CartService.toResponse} on every read + after
 * every mutation. The detector never mutates the cart — it only
 * reports. Mitigation (accept new price, remove line, lower quantity)
 * is a customer decision surfaced in the UI.
 *
 * <p><strong>Codes emitted:</strong>
 * <ul>
 *   <li>{@code product_removed} — the product has been soft-deleted
 *       since the line was added. Emitted only. Deleting the product
 *       hard is refused by the FK, so this only fires for
 *       {@code @SQLDelete} soft removals — the row is still there but
 *       merchant policy is "no longer for sale". Detected via the
 *       lifecycle status column, which reads as {@code ARCHIVED} or
 *       via {@code Product == null} if the ORM restriction filtered
 *       the join.</li>
 *   <li>{@code out_of_stock} — status is {@code OUT_OF_STOCK} OR
 *       {@code INACTIVE} OR {@code stockQuantity} is smaller than the
 *       line's quantity.</li>
 *   <li>{@code size_unavailable} — the line's size is no longer in
 *       {@code product.availableSizes}. The customer originally saw it
 *       as a valid choice.</li>
 *   <li>{@code price_changed} — the live product price no longer
 *       matches the line's snapshot price. Neither cheaper nor more
 *       expensive is silently accepted; the customer must
 *       acknowledge at checkout (Order pipeline enforces this).</li>
 * </ul>
 *
 * <p>Multiple issues can fire for a single line — a soft-deleted piece
 * whose price also changed emits both. The frontend can present them
 * grouped by line.
 *
 * <p><strong>Not emitted at this stage</strong>: {@code quantity_capped}
 * is a Stage 4 merge-time observation.
 */
@Component
public class CartIssueDetector {

    public List<CartIssueResponse> detect(Cart cart) {
        List<CartIssueResponse> issues = new ArrayList<>();
        for (CartItem item : cart.getItems()) {
            addIssuesFor(item, issues);
        }
        return issues;
    }

    private void addIssuesFor(CartItem item, List<CartIssueResponse> out) {
        Product product = item.getProduct();
        if (product == null) {
            out.add(issue(item, "product_removed",
                    "'%s' is no longer available in the atelier's catalogue."
                            .formatted(item.getSnapshotName())));
            return;
        }

        // ARCHIVED products are still queryable but merchant-signalled
        // "not for sale ever again" — treat as removed for cart UX.
        if (product.getStatus() == ProductStatus.ARCHIVED) {
            out.add(issue(item, "product_removed",
                    "'%s' has been retired and is no longer available."
                            .formatted(product.getName())));
            return;   // no point emitting stock/price on a removed line
        }

        // Availability: OoS status, INACTIVE, or stock has slipped below
        // the line's quantity. INACTIVE reads as "temporarily withdrawn"
        // per ProductStatus Javadoc — same customer message applies.
        boolean statusBlocks = product.getStatus() == ProductStatus.OUT_OF_STOCK
                || product.getStatus() == ProductStatus.INACTIVE;
        if (statusBlocks) {
            out.add(issue(item, "out_of_stock",
                    "'%s' is currently out of stock.".formatted(product.getName())));
        } else if (product.getStockQuantity() < item.getQuantity()) {
            out.add(issue(item, "out_of_stock",
                    "Only %d of '%s' remain — reduce this line's quantity."
                            .formatted(product.getStockQuantity(), product.getName())));
        }

        // Size still offered?
        if (product.getAvailableSizes() == null
                || !product.getAvailableSizes().contains(item.getSize())) {
            out.add(issue(item, "size_unavailable",
                    "Size '%s' is no longer offered for '%s'."
                            .formatted(item.getSize(), product.getName())));
        }

        // Price changed? Compare via BigDecimal.compareTo — never equals()
        // (100 vs 100.00 are not .equals() but ARE .compareTo == 0).
        if (item.getSnapshotPrice() == null
                || product.getPrice() == null) {
            return; // Missing data — nothing sensible to compare
        }
        if (item.getSnapshotPrice().compareTo(product.getPrice()) != 0) {
            out.add(issue(item, "price_changed",
                    "Price updated from %s to %s — please review before checkout."
                            .formatted(item.getSnapshotPrice().toPlainString(),
                                    product.getPrice().toPlainString())));
        }
    }

    private CartIssueResponse issue(CartItem item, String code, String message) {
        return new CartIssueResponse(item.getId(), code, message);
    }
}
