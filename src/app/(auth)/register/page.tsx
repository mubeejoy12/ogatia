"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState, type FormEvent } from "react";

import { useAuth } from "@/features/auth/AuthContext";
import * as authApi from "@/lib/api/auth";
import { ApiClientError, ApiValidationError } from "@/lib/api/errors";

/**
 * Register — luxury voice, minimal fields per D3 (email + password +
 * optional display name). On success we immediately sign the customer
 * in with the same credentials so they don't have to hop through the
 * sign-in page.
 *
 * <p>Uses {@code authApi.register} directly (rather than going through
 * {@code useAuth}) because registration is a two-step transaction —
 * create the account, then log in — and {@code AuthContext} exposes
 * only the second step.
 */
export default function RegisterPage() {
  const router = useRouter();
  const { login } = useAuth();

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [displayName, setDisplayName] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      await authApi.register({
        email,
        password,
        displayName: displayName.trim() || undefined,
      });
      // Immediately sign the new account in so we can go straight to /account.
      await login(email, password);
      router.replace("/account");
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <section className="mx-auto max-w-md px-6 py-24">
      <h1 className="font-display text-4xl text-ink">Open an account</h1>
      <p className="mt-3 text-ink/70">
        A short introduction — enough to speak with you about your commission.
      </p>

      <form onSubmit={onSubmit} className="mt-10 flex flex-col gap-5" noValidate>
        <label className="flex flex-col gap-2 text-sm text-ink">
          Email
          <input
            type="email"
            required
            autoComplete="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            className="rounded-none border border-ink/20 bg-transparent px-3 py-2 text-base text-ink focus:border-gold focus:outline-none"
          />
        </label>

        <label className="flex flex-col gap-2 text-sm text-ink">
          Password
          <input
            type="password"
            required
            autoComplete="new-password"
            minLength={8}
            maxLength={128}
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            className="rounded-none border border-ink/20 bg-transparent px-3 py-2 text-base text-ink focus:border-gold focus:outline-none"
          />
          <span className="text-xs text-ink/60">
            8–128 characters. No forced special-character rules.
          </span>
        </label>

        <label className="flex flex-col gap-2 text-sm text-ink">
          Name <span className="text-ink/50">(optional — how we'll greet you)</span>
          <input
            type="text"
            autoComplete="name"
            maxLength={120}
            value={displayName}
            onChange={(e) => setDisplayName(e.target.value)}
            className="rounded-none border border-ink/20 bg-transparent px-3 py-2 text-base text-ink focus:border-gold focus:outline-none"
          />
        </label>

        {error && (
          <p role="alert" className="text-sm text-red-700">
            {error}
          </p>
        )}

        <button
          type="submit"
          disabled={submitting || !email || !password}
          className="mt-2 bg-ink px-6 py-3 text-sm uppercase tracking-widest text-ivory transition hover:bg-ink/90 disabled:opacity-50"
        >
          {submitting ? "Opening…" : "Open Account"}
        </button>
      </form>

      <p className="mt-8 text-sm text-ink/70">
        Already have an account?{" "}
        <Link href="/login" className="text-gold underline underline-offset-4">
          Sign in
        </Link>
        .
      </p>
    </section>
  );
}

function errorMessage(err: unknown): string {
  if (err instanceof ApiValidationError) {
    // Backend validation_failed shape carries field-level details.
    const first = err.body?.details?.[0];
    if (first) return `${first.field}: ${first.message}`;
    return err.message;
  }
  if (err instanceof ApiClientError) {
    if (err.status === 409) return "An account already exists for that email.";
    if (err.status === 429) return "Too many attempts. Please wait and try again.";
    return err.message;
  }
  return "Something went wrong. Please try again.";
}
