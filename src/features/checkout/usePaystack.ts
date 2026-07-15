"use client";

import { useCallback, useEffect, useState } from "react";
import {
  loadPaystackScript,
  openPaystackPopup,
  type PaystackSetupOptions,
  type PaystackSuccess,
} from "@/lib/paystack";

type PaystackStatus =
  | "loading"
  | "ready"
  | "missing-key"
  | "script-error"
  | "processing"
  | "cancelled"
  | "error";

/**
 * Load the Paystack script on mount and expose a typed `pay()` action.
 *
 * Owns three concerns so the calling component doesn't have to:
 *  · Loading the external script (idempotent, once per page)
 *  · Guarding against a missing public key (reports "missing-key")
 *  · Translating popup callbacks into React state a UI can respond to
 */
export function usePaystack() {
  const publicKey = process.env.NEXT_PUBLIC_PAYSTACK_PUBLIC_KEY ?? "";
  const [status, setStatus] = useState<PaystackStatus>("loading");
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!publicKey) {
      setStatus("missing-key");
      return;
    }
    let cancelled = false;
    loadPaystackScript()
      .then(() => {
        if (!cancelled) setStatus("ready");
      })
      .catch((e: Error) => {
        if (!cancelled) {
          setStatus("script-error");
          setError(e.message);
        }
      });
    return () => {
      cancelled = true;
    };
  }, [publicKey]);

  const pay = useCallback(
    async (
      options: Omit<PaystackSetupOptions, "callback" | "onClose">,
      onSuccess: (response: PaystackSuccess) => void
    ) => {
      if (!publicKey) {
        setStatus("missing-key");
        return;
      }
      setStatus("processing");
      setError(null);
      try {
        await openPaystackPopup(publicKey, {
          ...options,
          callback: (response) => {
            setStatus("ready");
            onSuccess(response);
          },
          onClose: () => {
            // Only downgrade to "cancelled" if we haven't already succeeded.
            setStatus((prev) => (prev === "processing" ? "cancelled" : prev));
          },
        });
      } catch (e) {
        setStatus("error");
        setError(e instanceof Error ? e.message : "Payment could not start.");
      }
    },
    [publicKey]
  );

  return { status, error, pay };
}
