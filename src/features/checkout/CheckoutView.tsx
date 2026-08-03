"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { useRouter } from "next/navigation";
import { Button } from "@/components/ui/button";
import { useCart } from "@/features/cart/CartContext";
import { useAuth } from "@/features/auth/AuthContext";
import { AddressForm } from "./AddressForm";
import { CheckoutEmpty } from "./CheckoutEmpty";
import { DeliveryMethodSelector } from "./DeliveryMethodSelector";
import { OrderReview } from "./OrderReview";
import {
  getDeliveryMethod,
  type DeliveryMethodId,
} from "./deliveryMethods";
import type { ShippingAddress } from "./types";
import {
  createOrder,
  freshIdempotencyKey,
} from "@/lib/api/orders";
import { ApiAuthError, ApiClientError } from "@/lib/api/errors";
import type { ApiCreateOrderRequest } from "@/types/api/orders";

const DELIVERY_STORAGE_KEY = "eazicut:checkout:delivery:v1";

/**
 * The checkout orchestrator (B006 Stage 5).
 *
 * <p>Address + delivery method are collected as before, then submit
 * fires {@code POST /orders} with a scoped {@code Idempotency-Key}
 * header. On 201, redirect to {@code /orders/[reference]}. On 409
 * {@code price_mismatch}, an inline dialog shows the fresh total —
 * confirming re-submits with a NEW idempotency key. All other 4xx
 * / 5xx surface an in-context error message; the form state is
 * preserved.
 *
 * <p>Signed-out visitors are bounced to
 * {@code /login?next=/checkout}. Payment integration (B007) will
 * bolt on top of this: order lands in {@code PENDING_PAYMENT} and
 * the confirmation page invites payment.
 */
export function CheckoutView() {
  const cart = useCart();
  const router = useRouter();
  const { status, getAccessToken } = useAuth();

  const [hydrated, setHydrated] = useState(false);
  const [address, setAddress] = useState<ShippingAddress | null>(null);
  const [isAddressValid, setIsAddressValid] = useState(false);
  const [deliveryId, setDeliveryId] = useState<DeliveryMethodId | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [priceMismatch, setPriceMismatch] =
    useState<{ message: string } | null>(null);

  // Hold the idempotency key across a single user-initiated attempt
  // (including its network retries). A fresh key is minted on every
  // NEW user-initiated submit (initial click, confirm after
  // price_mismatch) so the server can't dedupe two legitimate
  // attempts into one order.
  const idempotencyKeyRef = useRef<string | null>(null);

  // Hydrate delivery choice from previous visit.
  useEffect(() => {
    try {
      const raw = window.localStorage.getItem(DELIVERY_STORAGE_KEY);
      if (raw) setDeliveryId(raw as DeliveryMethodId);
    } catch {
      /* ignore */
    }
    setHydrated(true);
  }, []);

  useEffect(() => {
    if (!hydrated) return;
    try {
      if (deliveryId) {
        window.localStorage.setItem(DELIVERY_STORAGE_KEY, deliveryId);
      } else {
        window.localStorage.removeItem(DELIVERY_STORAGE_KEY);
      }
    } catch {
      /* ignore */
    }
  }, [deliveryId, hydrated]);

  // Bounce anonymous visitors to /login?next=/checkout.
  useEffect(() => {
    if (status === "anonymous") {
      router.replace(`/login?next=${encodeURIComponent("/checkout")}`);
    }
  }, [status, router]);

  const handleAddressChange = useCallback(
    (next: ShippingAddress, valid: boolean) => {
      setAddress(next);
      setIsAddressValid(valid);
    },
    []
  );

  if (!hydrated || status === "loading") {
    return <div aria-hidden className="min-h-[50vh]" />;
  }
  if (status === "anonymous") {
    return <div aria-hidden className="min-h-[50vh]" />;
  }
  if (cart.lines.length === 0) {
    return <CheckoutEmpty />;
  }

  const method = getDeliveryMethod(deliveryId);
  const canSubmit =
    isAddressValid && method !== null && address !== null && !submitting;

  async function submitOnce(
    idempotencyKey: string,
    submitAddress: ShippingAddress,
    submitMethod: NonNullable<ReturnType<typeof getDeliveryMethod>>
  ) {
    setError(null);
    setPriceMismatch(null);
    setSubmitting(true);

    const total = cart.subtotal + submitMethod.price;
    const body: ApiCreateOrderRequest = {
      deliveryMethodId: submitMethod.id,
      shippingAddress: {
        fullName: submitAddress.fullName,
        email: submitAddress.email,
        phone: submitAddress.phone,
        addressLine1: submitAddress.addressLine1,
        addressLine2: submitAddress.addressLine2 || undefined,
        city: submitAddress.city,
        region: submitAddress.region || undefined,
        postalCode: submitAddress.postalCode || undefined,
        country: submitAddress.country,
      },
      // BigDecimal on the wire is a string; the server compareTo
      // ignores trailing-zero scale so "108000" matches "108000.0000".
      expectedTotal: String(total),
    };

    try {
      const token = getAccessToken();
      const order = await createOrder(token, idempotencyKey, body);
      // Server clears the cart transactionally — the CartContext will
      // reflect that on next hydration; the redirect leaves this
      // component so the stale in-memory cart doesn't matter.
      router.replace(`/orders/${encodeURIComponent(order.reference)}`);
    } catch (err) {
      handleSubmitError(err);
    } finally {
      setSubmitting(false);
    }
  }

  function handleSubmitError(err: unknown) {
    if (err instanceof ApiAuthError) {
      // Session expired mid-checkout — bounce to sign-in.
      router.replace(`/login?next=${encodeURIComponent("/checkout")}`);
      return;
    }
    if (err instanceof ApiClientError) {
      const slug = err.body?.error;
      if (err.status === 409 && slug === "price_mismatch") {
        // Fresh total sits in the message; render as a confirm dialog.
        setPriceMismatch({ message: err.message });
        // Invalidate the current key so a "Confirm at new total" mints a new one.
        idempotencyKeyRef.current = null;
        return;
      }
      if (err.status === 400 && slug === "cart_empty") {
        setError("Your bag is empty — add a piece before placing an order.");
        return;
      }
      if (err.status === 400 && slug === "missing_idempotency_key") {
        // Should never happen — the client always mints one.
        setError("Something went wrong preparing the order. Please try again.");
        return;
      }
      setError(err.message);
      return;
    }
    setError("Something went wrong. Please try again.");
  }

  const handleProceed = async () => {
    if (!canSubmit || !address || !method) return;
    // Fresh key per user-initiated confirmation.
    const key = freshIdempotencyKey();
    idempotencyKeyRef.current = key;
    await submitOnce(key, address, method);
  };

  const handleConfirmAtNewTotal = async () => {
    if (!address || !method) return;
    // Refresh cart totals before re-submitting; the customer needs the
    // NEW expectedTotal to match what the server just computed. Since
    // the cart is server-backed (B005), a fresh cart fetch would be
    // ideal — but useCart already reflects the latest server state on
    // context updates, so we simply retry with the updated cart.subtotal.
    const key = freshIdempotencyKey();
    idempotencyKeyRef.current = key;
    await submitOnce(key, address, method);
  };

  const proceedLabel = submitting ? "Placing order…" : "Place order";

  let hint: string | null = null;
  if (error) {
    hint = error;
  } else if (!isAddressValid && !method) {
    hint = "Complete shipping details and choose a delivery method to continue.";
  } else if (!isAddressValid) {
    hint = "Complete shipping details to continue.";
  } else if (!method) {
    hint = "Choose a delivery method to continue.";
  }

  return (
    <div className="grid grid-cols-1 lg:grid-cols-12 gap-10 lg:gap-16 items-start">
      <div className="lg:col-span-8 space-y-14">
        {/* Shipping */}
        <section aria-labelledby="ship-heading">
          <h2
            id="ship-heading"
            className="font-display text-2xl md:text-3xl tracking-tightest"
          >
            <span className="text-gold mr-3">01</span> Shipping details
          </h2>
          <p className="mt-3 text-stone-700 max-w-lg leading-relaxed">
            Where should we deliver the commission? A member of the atelier
            team will confirm every detail before shipping.
          </p>
          <div className="mt-10">
            <AddressForm onChange={handleAddressChange} />
          </div>
        </section>

        {/* Delivery */}
        <section aria-labelledby="delivery-heading">
          <h2
            id="delivery-heading"
            className="font-display text-2xl md:text-3xl tracking-tightest"
          >
            <span className="text-gold mr-3">02</span> Delivery method
          </h2>
          <p className="mt-3 text-stone-700 max-w-lg leading-relaxed">
            Insured shipping in signature ivory packaging. Choose the option
            that matches your journey.
          </p>
          <div className="mt-10">
            <DeliveryMethodSelector
              selected={deliveryId}
              onSelect={setDeliveryId}
            />
          </div>
        </section>

        {/* Continue */}
        <section
          aria-label="Continue"
          className="border-t border-ink/10 pt-10"
        >
          <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
            <div className="text-sm text-stone-700 max-w-sm">
              Your order lands with the atelier as{" "}
              <span className="font-semibold">Pending Payment</span>. Payment
              is confirmed in the next step.
            </div>
            <Button
              type="button"
              variant="primary"
              size="lg"
              disabled={!canSubmit}
              onClick={handleProceed}
              aria-disabled={!canSubmit}
              aria-describedby={hint ? "proceed-hint" : undefined}
            >
              {proceedLabel}
            </Button>
          </div>

          {hint && (
            <p
              id="proceed-hint"
              role={error ? "alert" : "status"}
              aria-live="polite"
              className={`mt-3 text-xs sm:text-right ${
                error ? "text-red-700" : "text-stone-500"
              }`}
            >
              {hint}
            </p>
          )}
        </section>
      </div>

      <div className="lg:col-span-4 lg:sticky lg:top-28">
        <OrderReview
          lines={cart.lines}
          subtotal={cart.subtotal}
          method={method}
        />
      </div>

      {/* Price-mismatch dialog */}
      {priceMismatch && (
        <div
          role="alertdialog"
          aria-labelledby="pm-heading"
          aria-modal="true"
          className="fixed inset-0 z-[100] flex items-center justify-center bg-ink/50 p-6"
        >
          <div className="bg-ivory max-w-md w-full p-8 shadow-xl">
            <h3
              id="pm-heading"
              className="font-display text-xl text-ink"
            >
              Prices have updated
            </h3>
            <p className="mt-4 text-sm text-stone-700 leading-relaxed">
              {priceMismatch.message}
            </p>
            <div className="mt-8 flex flex-col-reverse sm:flex-row gap-3 sm:justify-end">
              <Button
                type="button"
                variant="secondary"
                onClick={() => setPriceMismatch(null)}
              >
                Cancel
              </Button>
              <Button
                type="button"
                variant="primary"
                onClick={handleConfirmAtNewTotal}
                disabled={submitting}
              >
                {submitting ? "Placing order…" : "Confirm at new total"}
              </Button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
