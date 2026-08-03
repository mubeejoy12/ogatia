"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect } from "react";

import { useAuth } from "@/features/auth/AuthContext";

/**
 * Account landing — minimal stub for B004. Real order history,
 * measurement profile, and commission-in-progress panels arrive with
 * the Orders module ticket.
 *
 * <p>Client-side gate: {@code middleware.ts} deflects anonymous
 * visitors to {@code /login?next=/account} before this component
 * ever mounts. This effect covers the edge case where the middleware
 * pass-through (session marker present) turns out not to match a
 * live session (marker tampered / server-side revoked) — the mount-
 * time refresh in {@code AuthProvider} settles state to
 * {@code "anonymous"} and we bounce.
 */
export default function AccountPage() {
  const router = useRouter();
  const { status, user, logout } = useAuth();

  useEffect(() => {
    if (status === "anonymous") {
      router.replace("/login?next=/account");
    }
  }, [status, router]);

  if (status !== "authenticated" || !user) {
    return (
      <section className="mx-auto max-w-3xl px-6 py-24">
        <p className="text-ink/70">Loading…</p>
      </section>
    );
  }

  const greeting = user.displayName?.trim() || user.email.split("@")[0];

  return (
    <section className="mx-auto max-w-3xl px-6 py-24">
      <p className="text-xs uppercase tracking-widest text-gold">Your Atelier</p>
      <h1 className="mt-2 font-display text-4xl text-ink">Good to see you, {greeting}.</h1>

      <dl className="mt-10 grid grid-cols-1 gap-6 border-t border-ink/10 pt-8 sm:grid-cols-2">
        <div>
          <dt className="text-xs uppercase tracking-widest text-ink/60">Email</dt>
          <dd className="mt-1 text-ink">{user.email}</dd>
        </div>
        <div>
          <dt className="text-xs uppercase tracking-widest text-ink/60">Role</dt>
          <dd className="mt-1 text-ink">
            {user.role === "ADMIN" ? "Atelier — Administrator" : "Customer"}
          </dd>
        </div>
      </dl>

      <div className="mt-14 grid grid-cols-1 gap-6 border-t border-ink/10 pt-8 sm:grid-cols-2">
        <Link
          href="/account/orders"
          className="group block border border-ink/10 p-6 transition hover:border-ink"
        >
          <p className="text-xs uppercase tracking-widest text-gold">Orders</p>
          <p className="mt-2 font-display text-2xl text-ink">
            Your commissions
          </p>
          <p className="mt-2 text-sm text-ink/70">
            Every order you have placed, in one place.
          </p>
          <span className="mt-4 inline-block text-sm text-ink/70 group-hover:text-ink">
            View →
          </span>
        </Link>
      </div>

      <button
        onClick={() => {
          void logout().then(() => router.replace("/"));
        }}
        className="mt-10 border border-ink px-6 py-3 text-sm uppercase tracking-widest text-ink transition hover:bg-ink hover:text-ivory"
      >
        Sign out
      </button>
    </section>
  );
}
