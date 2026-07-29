"use client";

import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { Suspense, useState, type FormEvent } from "react";

import { useAuth } from "@/features/auth/AuthContext";
import { ApiAuthError, ApiClientError } from "@/lib/api/errors";

/**
 * Sign in — luxury voice, not "log in to your account".
 *
 * <p>Redirects to {@code ?next=} on success, defaulting to
 * {@code /account}. The {@code next} value is validated to be a
 * same-origin absolute path so a malicious link cannot bounce a
 * newly-signed-in customer to an off-site page.
 *
 * <p>The outer default export wraps the form in Suspense — Next.js
 * requires {@code useSearchParams} to sit inside a Suspense boundary
 * when the page is statically prerendered.
 */
export default function LoginPage() {
  return (
    <Suspense fallback={<LoginFrame>Loading…</LoginFrame>}>
      <LoginForm />
    </Suspense>
  );
}

function LoginFrame({ children }: { children: React.ReactNode }) {
  return (
    <section className="mx-auto max-w-md px-6 py-24">
      <h1 className="font-display text-4xl text-ink">Sign in to the Atelier</h1>
      <p className="mt-3 text-ink/70">
        A signed-in view keeps your commissions and correspondence in one place.
      </p>
      {children}
    </section>
  );
}

function LoginForm() {
  const router = useRouter();
  const search = useSearchParams();
  const { login } = useAuth();

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      await login(email, password);
      router.replace(safeNext(search.get("next")));
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <LoginFrame>
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
            autoComplete="current-password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
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
          {submitting ? "Signing in…" : "Sign In"}
        </button>
      </form>

      <p className="mt-8 text-sm text-ink/70">
        No account yet?{" "}
        <Link href="/register" className="text-gold underline underline-offset-4">
          Open one at the atelier
        </Link>
        .
      </p>
    </LoginFrame>
  );
}

/** Same-origin absolute paths only — prevents open-redirect abuse. */
function safeNext(raw: string | null): string {
  if (!raw) return "/account";
  if (!raw.startsWith("/") || raw.startsWith("//")) return "/account";
  return raw;
}

function errorMessage(err: unknown): string {
  if (err instanceof ApiAuthError) return "Invalid email or password.";
  if (err instanceof ApiClientError) {
    if (err.status === 429) return "Too many attempts. Please wait a few minutes and try again.";
    return err.message;
  }
  return "Something went wrong. Please try again.";
}
