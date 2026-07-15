"use client";

import { useCallback, useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { Button } from "@/components/ui/button";
import { useCart } from "@/features/cart/CartContext";
import { AddressForm } from "./AddressForm";
import { CheckoutEmpty } from "./CheckoutEmpty";
import { DeliveryMethodSelector } from "./DeliveryMethodSelector";
import { OrderReview } from "./OrderReview";
import {
  getDeliveryMethod,
  type DeliveryMethodId,
} from "./deliveryMethods";
import type { ShippingAddress } from "./types";
import { usePaystack } from "./usePaystack";
import { generateReference } from "@/lib/paystack";

const DELIVERY_STORAGE_KEY = "eazicut:checkout:delivery:v1";

/**
 * The checkout orchestrator.
 *
 * Stage 1: address form + empty guard.
 * Stage 2: delivery method + order review + "Continue to Payment" CTA,
 *          gated on address validity + delivery selection.
 *
 * The Continue button is wired to `onProceed` — currently a stub. Ticket
 * 003.2 replaces the stub with the Paystack initiation call. No markup
 * change required.
 */
export function CheckoutView() {
  const cart = useCart();
  const router = useRouter();
  const paystack = usePaystack();

  const [hydrated, setHydrated] = useState(false);
  const [address, setAddress] = useState<ShippingAddress | null>(null);
  const [isAddressValid, setIsAddressValid] = useState(false);
  const [deliveryId, setDeliveryId] = useState<DeliveryMethodId | null>(null);

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

  // Persist delivery choice on change.
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

  const handleAddressChange = useCallback(
    (next: ShippingAddress, valid: boolean) => {
      setAddress(next);
      setIsAddressValid(valid);
    },
    []
  );

  if (!hydrated) {
    return <div aria-hidden className="min-h-[50vh]" />;
  }

  if (cart.lines.length === 0) {
    return <CheckoutEmpty />;
  }

  const method = getDeliveryMethod(deliveryId);
  const paystackReady =
    paystack.status === "ready" ||
    paystack.status === "cancelled" ||
    paystack.status === "error";
  const canProceed =
    isAddressValid &&
    method !== null &&
    address !== null &&
    paystackReady &&
    paystack.status !== "processing";

  const isProcessing = paystack.status === "processing";

  const handleProceed = async () => {
    if (!canProceed || !address || !method) return;

    const total = cart.subtotal + method.price;
    const reference = generateReference();

    await paystack.pay(
      {
        email: address.email,
        amountNaira: total,
        reference,
        currency: "NGN",
        metadata: {
          customer_name: address.fullName,
          phone: address.phone,
          shipping_country: address.country,
          delivery_method: method.id,
          line_items: cart.lines.map((l) => ({
            slug: l.slug,
            size: l.size,
            quantity: l.quantity,
          })),
        },
      },
      (response) => {
        // Success — hand off to the confirmation route (Ticket 003.3).
        router.push(
          `/checkout/confirmation?ref=${encodeURIComponent(response.reference)}`
        );
      }
    );
  };

  const proceedLabel = isProcessing ? "Opening secure checkout…" : "Continue to Payment";

  // Compose the accessible hint under the button.
  let hint: string | null = null;
  if (paystack.status === "missing-key") {
    hint =
      "Payment provider is not configured. Set NEXT_PUBLIC_PAYSTACK_PUBLIC_KEY in .env.local.";
  } else if (paystack.status === "script-error") {
    hint = paystack.error ?? "Payment provider could not be reached. Please try again.";
  } else if (paystack.status === "cancelled") {
    hint = "Payment was cancelled. You can try again whenever you are ready.";
  } else if (paystack.status === "error") {
    hint = paystack.error ?? "Something went wrong starting the payment. Please try again.";
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
              You will be able to review and complete payment in the next step.
              No charge is made until you confirm.
            </div>
            <Button
              type="button"
              variant="primary"
              size="lg"
              disabled={!canProceed}
              onClick={handleProceed}
              aria-disabled={!canProceed}
              aria-describedby={hint ? "proceed-hint" : undefined}
            >
              {proceedLabel}
            </Button>
          </div>

          {hint && (
            <p
              id="proceed-hint"
              role="status"
              aria-live="polite"
              className="mt-3 text-xs text-stone-500 sm:text-right"
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
    </div>
  );
}
