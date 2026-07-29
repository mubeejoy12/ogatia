"use client";

import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useRef,
  useState,
  type ReactNode,
} from "react";

import type { ApiUserResponse } from "@/types/api/auth";
import * as authApi from "@/lib/api/auth";
import { ApiAuthError } from "@/lib/api/errors";

/**
 * Global auth state for the client bundle.
 *
 * <p><strong>Access token lives in memory only.</strong> Storing it in
 * {@code localStorage} would expose it to any XSS on the site;
 * keeping it in a React ref means it dies with the tab and never
 * touches persistent storage. The trade-off — a full-page reload
 * loses the token — is closed by the mount-time silent refresh
 * against the HttpOnly refresh cookie.
 *
 * <p><strong>Refresh cookie lives in the browser's HttpOnly jar.</strong>
 * We never see the raw value; the browser attaches it automatically
 * on {@code POST /api/v1/auth/refresh} (path-scoped) and the backend
 * rotates it.
 *
 * <p><strong>Hydration order on first mount:</strong>
 * <ol>
 *   <li>State starts {@code status = "loading"}, no user.</li>
 *   <li>{@code refresh()} tries the cookie. On success, we receive
 *       a fresh access token + user; state moves to
 *       {@code "authenticated"}.</li>
 *   <li>On 401 (missing/expired/revoked cookie), state moves to
 *       {@code "anonymous"} — the normal signed-out UI.</li>
 * </ol>
 *
 * Consumers hook into {@link useAuth} for state + actions. Read-only
 * consumers should treat {@code status === "loading"} as "don't
 * render session-dependent UI yet" (the first paint is already
 * anonymous-shaped, so skeleton-hiding is cheap).
 */
export type AuthStatus = "loading" | "authenticated" | "anonymous";

export type AuthState = {
  status: AuthStatus;
  user: ApiUserResponse | null;
};

export type AuthActions = {
  /** Log in with email + password. Throws on failure. */
  login: (email: string, password: string) => Promise<void>;
  /** Log out and clear both cookies + in-memory token. Idempotent. */
  logout: () => Promise<void>;
  /**
   * Force a re-fetch of {@code /auth/me}. Used by pages that want
   * to reflect a mutation (e.g. profile edit) immediately.
   */
  reloadMe: () => Promise<void>;
  /**
   * Latest access token. Consumers building their own {@code fetch}
   * calls should pull it via {@link useAccessToken} instead — this
   * accessor exists for the API client's 401 retry.
   */
  getAccessToken: () => string | null;
};

const AuthContext = createContext<(AuthState & AuthActions) | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [state, setState] = useState<AuthState>({ status: "loading", user: null });
  const accessTokenRef = useRef<string | null>(null);

  const applyLogin = useCallback((token: string, user: ApiUserResponse) => {
    accessTokenRef.current = token;
    setState({ status: "authenticated", user });
  }, []);

  const applyAnonymous = useCallback(() => {
    accessTokenRef.current = null;
    setState({ status: "anonymous", user: null });
  }, []);

  const login = useCallback(async (email: string, password: string) => {
    const result = await authApi.login({ email, password });
    applyLogin(result.accessToken, result.user);
  }, [applyLogin]);

  const logout = useCallback(async () => {
    try {
      await authApi.logout();
    } catch {
      // Logout is idempotent on the backend and we're about to
      // clear local state regardless — silence errors so a
      // network blip during sign-out doesn't leave the UI stuck.
    }
    applyAnonymous();
  }, [applyAnonymous]);

  const reloadMe = useCallback(async () => {
    try {
      const user = await authApi.me(accessTokenRef.current);
      setState({ status: "authenticated", user });
    } catch {
      applyAnonymous();
    }
  }, [applyAnonymous]);

  // Mount-time silent refresh — trades a cookie for a fresh access
  // token so a page reload doesn't force a re-login.
  useEffect(() => {
    let cancelled = false;
    (async () => {
      try {
        const result = await authApi.refresh();
        if (!cancelled) applyLogin(result.accessToken, result.user);
      } catch {
        if (!cancelled) applyAnonymous();
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [applyLogin, applyAnonymous]);

  const value = useMemo<AuthState & AuthActions>(
    () => ({
      ...state,
      login,
      logout,
      reloadMe,
      getAccessToken: () => accessTokenRef.current,
    }),
    [state, login, logout, reloadMe],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthState & AuthActions {
  const ctx = useContext(AuthContext);
  if (!ctx) {
    throw new Error("useAuth must be called inside <AuthProvider>.");
  }
  return ctx;
}

/** Convenience: subscribe to the current access token only. */
export function useAccessToken(): string | null {
  const { getAccessToken, status } = useAuth();
  // status is included as a dep-proxy so consumers re-render when it changes
  return status === "authenticated" ? getAccessToken() : null;
}

/** ApiAuthError re-export so callers don't need two imports. */
export { ApiAuthError };
