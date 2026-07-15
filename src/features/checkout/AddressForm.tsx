"use client";

import { useCallback, useEffect, useState } from "react";
import { Input } from "@/components/ui/input";
import { Select } from "@/components/ui/select";
import { Label } from "@/components/ui/label";
import {
  emptyAddress,
  shippingCountries,
  type AddressErrors,
  type ShippingAddress,
  type ShippingCountry,
} from "./types";
import { validateAddress } from "./validation";

const STORAGE_KEY = "eazicut:checkout:address:v1";

/**
 * Shipping / billing address form.
 *
 * Persists to localStorage so a refresh — or a return visit — does not
 * lose entered detail. Emits changes and validity upward via callbacks so
 * the enclosing CheckoutView owns overall step gating (Stage 2 will use
 * the `isValid` signal to unlock the payment step).
 */
export function AddressForm({
  onChange,
}: {
  onChange?: (address: ShippingAddress, isValid: boolean) => void;
}) {
  const [address, setAddress] = useState<ShippingAddress>(emptyAddress);
  const [errors, setErrors] = useState<AddressErrors>({});
  const [touched, setTouched] = useState<Record<string, boolean>>({});
  const [hydrated, setHydrated] = useState(false);

  // Hydrate persisted address after mount (no SSR access to localStorage).
  useEffect(() => {
    try {
      const raw = window.localStorage.getItem(STORAGE_KEY);
      if (raw) {
        const parsed = JSON.parse(raw) as Partial<ShippingAddress>;
        setAddress({ ...emptyAddress, ...parsed });
      }
    } catch {
      // ignore quota / private-mode errors
    }
    setHydrated(true);
  }, []);

  // Persist + report validity after each change (post-hydration only).
  useEffect(() => {
    if (!hydrated) return;
    try {
      window.localStorage.setItem(STORAGE_KEY, JSON.stringify(address));
    } catch {
      /* ignore */
    }
    const nextErrors = validateAddress(address);
    setErrors(nextErrors);
    onChange?.(address, Object.keys(nextErrors).length === 0);
  }, [address, hydrated, onChange]);

  const set = useCallback(
    <K extends keyof ShippingAddress>(key: K, value: ShippingAddress[K]) =>
      setAddress((a) => ({ ...a, [key]: value })),
    []
  );

  const markTouched = useCallback((key: keyof ShippingAddress) => {
    setTouched((t) => ({ ...t, [key]: true }));
  }, []);

  const errorFor = (key: keyof ShippingAddress) =>
    touched[key] ? errors[key] : undefined;

  return (
    <div className="space-y-8">
      <Field
        id="fullName"
        label="Full name"
        required
        value={address.fullName}
        onChange={(v) => set("fullName", v)}
        onBlur={() => markTouched("fullName")}
        error={errorFor("fullName")}
        autoComplete="name"
      />

      <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
        <Field
          id="email"
          label="Email"
          type="email"
          required
          value={address.email}
          onChange={(v) => set("email", v)}
          onBlur={() => markTouched("email")}
          error={errorFor("email")}
          autoComplete="email"
        />
        <Field
          id="phone"
          label="Phone"
          type="tel"
          required
          value={address.phone}
          onChange={(v) => set("phone", v)}
          onBlur={() => markTouched("phone")}
          error={errorFor("phone")}
          autoComplete="tel"
          placeholder="+234…"
        />
      </div>

      <Field
        id="addressLine1"
        label="Street address"
        required
        value={address.addressLine1}
        onChange={(v) => set("addressLine1", v)}
        onBlur={() => markTouched("addressLine1")}
        error={errorFor("addressLine1")}
        autoComplete="address-line1"
      />

      <Field
        id="addressLine2"
        label="Apartment, suite, or unit (optional)"
        value={address.addressLine2}
        onChange={(v) => set("addressLine2", v)}
        autoComplete="address-line2"
      />

      <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
        <Field
          id="city"
          label="City"
          required
          value={address.city}
          onChange={(v) => set("city", v)}
          onBlur={() => markTouched("city")}
          error={errorFor("city")}
          autoComplete="address-level2"
        />
        <Field
          id="region"
          label="State / Region"
          required
          value={address.region}
          onChange={(v) => set("region", v)}
          onBlur={() => markTouched("region")}
          error={errorFor("region")}
          autoComplete="address-level1"
        />
        <Field
          id="postalCode"
          label="Postal code (optional)"
          value={address.postalCode}
          onChange={(v) => set("postalCode", v)}
          autoComplete="postal-code"
        />
      </div>

      <div>
        <Label htmlFor="country" className="mb-3">
          Country <span className="text-gold">*</span>
        </Label>
        <Select
          id="country"
          value={address.country}
          onChange={(e) => set("country", e.target.value as ShippingCountry)}
          onBlur={() => markTouched("country")}
          autoComplete="country-name"
        >
          {shippingCountries.map((c) => (
            <option key={c} value={c}>
              {c}
            </option>
          ))}
        </Select>
        {errorFor("country") && (
          <p className="mt-2 text-xs text-gold-deep">{errorFor("country")}</p>
        )}
      </div>
    </div>
  );
}

// -----------------------------------------------------------------------------
// Small internal Field wrapper — keeps the form body readable
// -----------------------------------------------------------------------------

function Field({
  id,
  label,
  value,
  onChange,
  onBlur,
  error,
  type = "text",
  required = false,
  autoComplete,
  placeholder,
}: {
  id: string;
  label: string;
  value: string;
  onChange: (value: string) => void;
  onBlur?: () => void;
  error?: string;
  type?: string;
  required?: boolean;
  autoComplete?: string;
  placeholder?: string;
}) {
  return (
    <div>
      <Label htmlFor={id} className="mb-3">
        {label}
        {required && <span className="text-gold ml-1">*</span>}
      </Label>
      <Input
        id={id}
        name={id}
        type={type}
        value={value}
        required={required}
        aria-invalid={error ? true : undefined}
        aria-describedby={error ? `${id}-error` : undefined}
        autoComplete={autoComplete}
        placeholder={placeholder}
        onChange={(e) => onChange(e.target.value)}
        onBlur={onBlur}
      />
      {error && (
        <p id={`${id}-error`} className="mt-2 text-xs text-gold-deep">
          {error}
        </p>
      )}
    </div>
  );
}
