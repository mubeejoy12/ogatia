package com.eazicut.api.orders.service;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Component;

/**
 * Backend catalogue of shipping options, mirroring the frontend's
 * {@code src/features/checkout/deliveryMethods.ts}.
 *
 * <p><strong>Why hardcode?</strong> The launch set is four fixed
 * options — mirroring them here avoids a full delivery-methods table
 * for a value that changes about once a quarter. When shipping
 * partner integration or per-country dynamic quoting arrives, this
 * class becomes a facade over the same real service the frontend
 * consumes; the {@link Method} shape stays identical so no order
 * code moves.
 *
 * <p>Prices are in whole Naira (matching the frontend). The Order
 * pipeline still uses {@link BigDecimal} for the DB write.
 *
 * <p><strong>Kept in sync manually.</strong> Any change here must
 * land in {@code deliveryMethods.ts} in the same commit — enforced
 * by a checklist in the B007 audit's known-limitations block.
 */
@Component
public class DeliveryMethodCatalog {

    /**
     * Small immutable value carrier. Not a DTO — the Order pipeline
     * denormalises {@code id} and {@code name} onto the Order row so
     * a later rename doesn't rewrite history.
     */
    public record Method(String id, String name, BigDecimal cost) {}

    private static final Map<String, Method> METHODS = Map.of(
            "lagos-standard",
            new Method("lagos-standard", "Lagos delivery", new BigDecimal("8000")),
            "nigeria-nationwide",
            new Method("nigeria-nationwide", "Nigeria nationwide", new BigDecimal("15000")),
            "international-dhl",
            new Method("international-dhl", "International · DHL insured", new BigDecimal("45000")),
            "atelier-pickup",
            new Method("atelier-pickup", "Atelier pickup", BigDecimal.ZERO)
    );

    public Optional<Method> find(String id) {
        if (id == null) return Optional.empty();
        return Optional.ofNullable(METHODS.get(id));
    }
}
