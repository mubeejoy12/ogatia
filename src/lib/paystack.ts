/**
 * Paystack Inline JS wrapper.
 *
 * Loads https://js.paystack.co/v1/inline.js on demand (never during SSR),
 * provides a typed API surface for the popup, and handles the two
 * conversions every consumer must get right:
 *
 *   • Amount → kobo (₦1 = 100 kobo). Passing Naira directly charges 100×.
 *   • Reference → unique, prefixed, uppercase; the atelier greps for
 *     "EAZI-" in the Paystack dashboard.
 *
 * Server-side verification of a completed payment (via webhook) belongs
 * with the backend milestone. Until then, the atelier verifies each
 * reference manually on the Paystack dashboard.
 */

const PAYSTACK_SCRIPT_SRC = "https://js.paystack.co/v1/inline.js";

// -----------------------------------------------------------------------------
// Public types
// -----------------------------------------------------------------------------

export type PaystackChannel =
  | "card"
  | "bank"
  | "ussd"
  | "qr"
  | "mobile_money"
  | "bank_transfer";

export type PaystackSetupOptions = {
  email: string;
  /** Amount in whole Naira. Converted to kobo internally. */
  amountNaira: number;
  reference: string;
  currency?: "NGN";
  channels?: PaystackChannel[];
  metadata?: Record<string, unknown>;
  callback: (response: PaystackSuccess) => void;
  onClose: () => void;
};

export type PaystackSuccess = {
  reference: string;
  status: string;
  trans?: string;
  transaction?: string;
  message?: string;
};

// -----------------------------------------------------------------------------
// Ambient — Paystack attaches itself to `window`
// -----------------------------------------------------------------------------

type PaystackInternalOptions = {
  key: string;
  email: string;
  amount: number;
  currency?: string;
  ref: string;
  channels?: PaystackChannel[];
  metadata?: Record<string, unknown>;
  callback: (response: PaystackSuccess) => void;
  onClose: () => void;
};

type PaystackPop = {
  setup: (options: PaystackInternalOptions) => {
    openIframe: () => void;
  };
};

declare global {
  interface Window {
    PaystackPop?: PaystackPop;
  }
}

// -----------------------------------------------------------------------------
// Helpers
// -----------------------------------------------------------------------------

/**
 * Convert whole Naira to kobo, the unit Paystack expects.
 * Rounded to eliminate any floating-point residue.
 */
export const nairaToKobo = (naira: number): number => Math.round(naira * 100);

/**
 * Generate a unique, uppercase reference prefixed for atelier searchability.
 * Example:  EAZI-1720617483471-A9F3C1
 */
export function generateReference(): string {
  const ts = Date.now();
  const rand = Math.random().toString(36).slice(2, 8).toUpperCase();
  return `EAZI-${ts}-${rand}`;
}

// -----------------------------------------------------------------------------
// Script loader — idempotent, SSR-safe
// -----------------------------------------------------------------------------

let scriptPromise: Promise<void> | null = null;

export function loadPaystackScript(): Promise<void> {
  if (typeof window === "undefined") {
    return Promise.reject(new Error("Paystack cannot load on the server."));
  }
  if (window.PaystackPop) return Promise.resolve();
  if (scriptPromise) return scriptPromise;

  scriptPromise = new Promise<void>((resolve, reject) => {
    const existing = document.querySelector<HTMLScriptElement>(
      `script[src="${PAYSTACK_SCRIPT_SRC}"]`
    );
    if (existing) {
      existing.addEventListener("load", () => resolve());
      existing.addEventListener("error", () =>
        reject(new Error("Failed to load Paystack script."))
      );
      return;
    }

    const script = document.createElement("script");
    script.src = PAYSTACK_SCRIPT_SRC;
    script.async = true;
    script.onload = () => resolve();
    script.onerror = () => {
      scriptPromise = null; // allow retry
      reject(new Error("Failed to load Paystack script."));
    };
    document.head.appendChild(script);
  });

  return scriptPromise;
}

// -----------------------------------------------------------------------------
// Public API — the only surface consumers should touch
// -----------------------------------------------------------------------------

/**
 * Open the Paystack payment popup.
 *
 * @throws if the public key is missing (i.e. env var not configured) or
 *         if the Paystack script fails to load / initialise.
 */
export async function openPaystackPopup(
  publicKey: string,
  options: PaystackSetupOptions
): Promise<void> {
  if (!publicKey) {
    throw new Error(
      "Paystack public key is missing. Set NEXT_PUBLIC_PAYSTACK_PUBLIC_KEY in .env.local."
    );
  }

  await loadPaystackScript();

  if (!window.PaystackPop) {
    throw new Error("PaystackPop failed to initialise on window.");
  }

  const handler = window.PaystackPop.setup({
    key: publicKey,
    email: options.email,
    amount: nairaToKobo(options.amountNaira),
    currency: options.currency ?? "NGN",
    ref: options.reference,
    channels: options.channels,
    metadata: options.metadata,
    callback: options.callback,
    onClose: options.onClose,
  });

  handler.openIframe();
}
