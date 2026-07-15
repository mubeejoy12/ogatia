"use client";

import { useCallback, useEffect, useState } from "react";
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
  const canProceed = isAddressValid && method !== null;

  const handleProceed = () => {
    // TODO(003.2): initialise Paystack with { address, method, cart } here.
    // Left as a stub so the button renders and the gate logic is verifiable
    // in Stage 2. Wired up in Ticket 003.2 without markup change.
    if (!canProceed) return;
  };

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
              /* Address + delivery both known — reference the gate for a11y */
              aria-describedby={!canProceed ? "proceed-hint" : undefined}
            >
              Continue to Payment
            </Button>
          </div>

          {!canProceed && (
            <p
              id="proceed-hint"
              className="mt-3 text-xs text-stone-500 sm:text-right"
            >
              {!isAddressValid && !method
                ? "Complete shipping details and choose a delivery method to continue."
                : !isAddressValid
                  ? "Complete shipping details to continue."
                  : "Choose a delivery method to continue."}
            </p>
          )}

          {/* Suppress unused-var lint until Ticket 003.2 wires the address into
              the Paystack initiation payload. */}
          <span className="sr-only">{address?.email ?? ""}</span>
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
