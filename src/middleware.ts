import { NextResponse, type NextRequest } from "next/server";

/**
 * Route middleware — light session gate for {@code /account}.
 *
 * <p>Reads {@code eazicut_session} — a non-HttpOnly, non-credential
 * presence cookie the backend sets alongside the real refresh cookie
 * on login. Its purpose is to answer "is this browser plausibly
 * signed in?" without exposing the actual refresh token to Next's
 * middleware runtime (which would defeat the whole HttpOnly point).
 *
 * <p>Marker present → let the request through; the {@code /account}
 * client component re-verifies against {@code /auth/me} and bounces
 * to {@code /login} if the marker turns out to be stale (server
 * revoked, cookie tampered).
 *
 * <p>Marker missing → 302 redirect straight to
 * {@code /login?next=<original>} so the sign-in flow returns the
 * customer to what they were doing.
 *
 * <p>Kept intentionally narrow via the {@code matcher} — we do NOT
 * want to run this filter on every request (image assets, product
 * pages, etc.).
 */
export function middleware(request: NextRequest) {
  const hasSession = request.cookies.get("eazicut_session")?.value === "1";
  if (hasSession) {
    return NextResponse.next();
  }

  const next = request.nextUrl.pathname + request.nextUrl.search;
  const login = new URL("/login", request.url);
  login.searchParams.set("next", next);
  return NextResponse.redirect(login);
}

export const config = {
  matcher: ["/account/:path*", "/orders/:path*", "/checkout/:path*"],
};
