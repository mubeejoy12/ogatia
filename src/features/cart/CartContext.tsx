"use client";

import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useReducer,
  useRef,
  useState,
  type ReactNode,
} from "react";

import type {
  CartApi,
  CartLine,
  CartMode,
  CartSnapshot,
  CartState,
} from "./types";
import { useAuth } from "@/features/auth/AuthContext";
import * as cartApi from "@/lib/api/cart";
import type {
  ApiCart,
  ApiCartIssue,
  ApiCartItem,
  ApiMergeCartLine,
} from "@/types/api/cart";

// ---------------------------------------------------------------------------
// Reducer — used by guest mode; server mode replaces state atomically
// from every API response so the reducer isn't consulted there.
// ---------------------------------------------------------------------------

type Action =
  | { type: "hydrate"; state: CartState }
  | {
      type: "add";
      snapshot: CartSnapshot;
      size: string;
      quantity: number;
    }
  | { type: "remove"; id: string }
  | { type: "setQuantity"; id: string; quantity: number }
  | { type: "clear" };

const initialState: CartState = { lines: [] };

const lineId = (slug: string, size: string) => `${slug}::${size}`;

function reducer(state: CartState, action: Action): CartState {
  switch (action.type) {
    case "hydrate":
      return action.state;

    case "add": {
      const id = lineId(action.snapshot.slug, action.size);
      const existing = state.lines.find((l) => l.id === id);
      if (existing) {
        return {
          lines: state.lines.map((l) =>
            l.id === id ? { ...l, quantity: l.quantity + action.quantity } : l
          ),
        };
      }
      const line: CartLine = {
        id,
        slug: action.snapshot.slug,
        size: action.size,
        quantity: action.quantity,
        addedAt: new Date().toISOString(),
        snapshot: action.snapshot,
      };
      return { lines: [...state.lines, line] };
    }

    case "remove":
      return { lines: state.lines.filter((l) => l.id !== action.id) };

    case "setQuantity":
      if (action.quantity <= 0) {
        return { lines: state.lines.filter((l) => l.id !== action.id) };
      }
      return {
        lines: state.lines.map((l) =>
          l.id === action.id ? { ...l, quantity: action.quantity } : l
        ),
      };

    case "clear":
      return initialState;
  }
}

// ---------------------------------------------------------------------------
// LocalStorage persistence — guest mode only
// ---------------------------------------------------------------------------

const STORAGE_KEY = "eazicut:cart:v1";

function readStorage(): CartState | null {
  if (typeof window === "undefined") return null;
  try {
    const raw = window.localStorage.getItem(STORAGE_KEY);
    if (!raw) return null;
    const parsed = JSON.parse(raw) as CartState;
    if (!Array.isArray(parsed.lines)) return null;
    return parsed;
  } catch {
    return null;
  }
}

function writeStorage(state: CartState) {
  if (typeof window === "undefined") return;
  try {
    window.localStorage.setItem(STORAGE_KEY, JSON.stringify(state));
  } catch {
    /* quota / private mode — ignore */
  }
}

function clearStorage() {
  if (typeof window === "undefined") return;
  try {
    window.localStorage.removeItem(STORAGE_KEY);
  } catch {
    /* ignore */
  }
}

// ---------------------------------------------------------------------------
// Server cart → local CartLine adapter (server mode)
// ---------------------------------------------------------------------------

function apiItemToLine(item: ApiCartItem): CartLine {
  return {
    id: item.id,
    slug: item.productSlug,
    size: item.size,
    quantity: item.quantity,
    addedAt: item.addedAt,
    snapshot: {
      productId: item.productId,
      slug: item.productSlug,
      name: item.snapshot.name,
      price: Number(item.snapshot.price),
      // Wrap the server's URL into the ImageAsset shape the UI expects.
      image: {
        src: item.snapshot.imageUrl ?? "",
        alt: item.snapshot.name,
      },
      // Collection + category aren't persisted on the server line —
      // Product carries them; the snapshot is intentionally lean.
      // The cast keeps TypeScript happy; Cart page + Navbar don't
      // depend on these fields for server-mode lines.
      collection: "" as CartLine["snapshot"]["collection"],
      category: "" as CartLine["snapshot"]["category"],
    },
  };
}

function apiCartToState(cart: ApiCart): CartState {
  return { lines: cart.items.map(apiItemToLine) };
}

// ---------------------------------------------------------------------------
// Context
// ---------------------------------------------------------------------------

const CartContext = createContext<CartApi | null>(null);

export function CartProvider({ children }: { children: ReactNode }) {
  const { status, getAccessToken } = useAuth();

  const [state, dispatch] = useReducer(reducer, initialState);
  const [issues, setIssues] = useState<ApiCartIssue[]>([]);
  const [hydrated, setHydrated] = useState(false);

  // Which persistence surface owns the current state:
  //  - "guest"  → reducer + localStorage
  //  - "server" → API responses replace state atomically
  const modeRef = useRef<CartMode>("guest");

  // -------------------------------------------------------------------
  // Anonymous → hydrate from localStorage (guest mode)
  // -------------------------------------------------------------------

  useEffect(() => {
    if (status !== "anonymous") return;
    modeRef.current = "guest";
    const persisted = readStorage();
    dispatch({ type: "hydrate", state: persisted ?? initialState });
    setIssues([]);
    setHydrated(true);
  }, [status]);

  // Guest-mode write on every change.
  useEffect(() => {
    if (modeRef.current === "guest" && hydrated) writeStorage(state);
  }, [state, hydrated]);

  // -------------------------------------------------------------------
  // Authenticated → one-shot merge (if guest lines present) OR plain
  // GET /cart, then future mutations hit the server.
  // -------------------------------------------------------------------

  useEffect(() => {
    if (status !== "authenticated") return;
    let cancelled = false;
    (async () => {
      const token = getAccessToken();
      if (!token) return;

      // Read guest lines BEFORE any reshape — the merge payload needs
      // them. If there are none, /cart/merge is skipped; a plain GET
      // is enough.
      const guest = readStorage();
      const mergeLines: ApiMergeCartLine[] =
        guest?.lines
          ?.filter((l) => l.slug && l.size && l.quantity > 0)
          .map((l) => ({
            productSlug: l.slug,
            size: l.size,
            quantity: l.quantity,
          })) ?? [];

      try {
        const server = mergeLines.length > 0
          ? await cartApi.mergeCart(token, { lines: mergeLines })
          : await cartApi.getCart(token);
        if (cancelled) return;
        modeRef.current = "server";
        dispatch({ type: "hydrate", state: apiCartToState(server) });
        setIssues(server.issues);
        clearStorage();
        setHydrated(true);
      } catch (err) {
        console.error("[cart] failed to hydrate server cart", err);
        // Fall back to guest cart so the UI still works.
        modeRef.current = "guest";
        dispatch({ type: "hydrate", state: readStorage() ?? initialState });
        setHydrated(true);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [status, getAccessToken]);

  // Logout → wipe in-memory server-cart view; don't touch localStorage
  // (a fresh guest cart begins empty, but any pre-login guest cart
  // was already cleared at merge time).
  const prevStatus = useRef(status);
  useEffect(() => {
    if (prevStatus.current === "authenticated" && status === "anonymous") {
      modeRef.current = "guest";
      dispatch({ type: "clear" });
      setIssues([]);
    }
    prevStatus.current = status;
  }, [status]);

  // -------------------------------------------------------------------
  // Public actions — same signatures as before B005 so PDP + checkout
  // don't need to change. Server mode is fire-and-forget; state
  // updates when the API responds.
  // -------------------------------------------------------------------

  const applyServer = useCallback((cart: ApiCart) => {
    dispatch({ type: "hydrate", state: apiCartToState(cart) });
    setIssues(cart.issues);
  }, []);

  const add = useCallback<CartApi["add"]>(
    (snapshot, size, quantity = 1) => {
      if (modeRef.current === "server") {
        const token = getAccessToken();
        const productId = snapshot.productId;
        if (!token || !productId) {
          console.error("[cart] server add requires token + productId");
          return;
        }
        void cartApi
          .addCartItem(token, { productId, size, quantity })
          .then(applyServer)
          .catch((err) => console.error("[cart] add failed", err));
        return;
      }
      dispatch({ type: "add", snapshot, size, quantity });
    },
    [applyServer, getAccessToken]
  );

  const remove = useCallback<CartApi["remove"]>(
    (id) => {
      if (modeRef.current === "server") {
        const token = getAccessToken();
        if (!token) return;
        void cartApi
          .removeCartItem(token, id)
          .then(applyServer)
          .catch((err) => console.error("[cart] remove failed", err));
        return;
      }
      dispatch({ type: "remove", id });
    },
    [applyServer, getAccessToken]
  );

  const setQuantity = useCallback<CartApi["setQuantity"]>(
    (id, quantity) => {
      if (modeRef.current === "server") {
        const token = getAccessToken();
        if (!token) return;
        if (quantity <= 0) {
          void cartApi
            .removeCartItem(token, id)
            .then(applyServer)
            .catch((err) => console.error("[cart] remove failed", err));
        } else {
          void cartApi
            .updateCartItem(token, id, { quantity })
            .then(applyServer)
            .catch((err) => console.error("[cart] patch failed", err));
        }
        return;
      }
      dispatch({ type: "setQuantity", id, quantity });
    },
    [applyServer, getAccessToken]
  );

  const clear = useCallback<CartApi["clear"]>(
    () => {
      if (modeRef.current === "server") {
        const token = getAccessToken();
        if (!token) return;
        void cartApi
          .clearCart(token)
          .then(applyServer)
          .catch((err) => console.error("[cart] clear failed", err));
        return;
      }
      dispatch({ type: "clear" });
    },
    [applyServer, getAccessToken]
  );

  const value = useMemo<CartApi>(() => {
    const count = state.lines.reduce((sum, l) => sum + l.quantity, 0);
    const subtotal = state.lines.reduce(
      (sum, l) => sum + l.snapshot.price * l.quantity,
      0
    );
    return {
      lines: state.lines,
      count,
      subtotal,
      mode: modeRef.current,
      issues,
      add,
      remove,
      setQuantity,
      clear,
    };
  }, [state, issues, add, remove, setQuantity, clear]);

  return <CartContext.Provider value={value}>{children}</CartContext.Provider>;
}

/**
 * Consumer hook.
 *
 * Throws if used outside `<CartProvider>` — which would indicate a wiring
 * mistake worth surfacing loudly rather than falling back silently.
 */
export function useCart(): CartApi {
  const ctx = useContext(CartContext);
  if (!ctx) throw new Error("useCart must be used within a CartProvider");
  return ctx;
}
