"use client";

import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useReducer,
  useState,
  type ReactNode,
} from "react";
import type {
  CartApi,
  CartLine,
  CartSnapshot,
  CartState,
} from "./types";

// ---------------------------------------------------------------------------
// Reducer — pure, testable
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
// LocalStorage persistence — SSR-safe
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
    /* quota errors, private mode — ignore */
  }
}

// ---------------------------------------------------------------------------
// Context
// ---------------------------------------------------------------------------

const CartContext = createContext<CartApi | null>(null);

export function CartProvider({ children }: { children: ReactNode }) {
  const [state, dispatch] = useReducer(reducer, initialState);
  const [hydrated, setHydrated] = useState(false);

  // Hydrate from localStorage on mount — never during SSR, so no hydration mismatch.
  useEffect(() => {
    const persisted = readStorage();
    if (persisted) dispatch({ type: "hydrate", state: persisted });
    setHydrated(true);
  }, []);

  // Write on every change, but never before hydration completes.
  useEffect(() => {
    if (hydrated) writeStorage(state);
  }, [state, hydrated]);

  const add = useCallback<CartApi["add"]>(
    (snapshot, size, quantity = 1) =>
      dispatch({ type: "add", snapshot, size, quantity }),
    []
  );
  const remove = useCallback<CartApi["remove"]>(
    (id) => dispatch({ type: "remove", id }),
    []
  );
  const setQuantity = useCallback<CartApi["setQuantity"]>(
    (id, quantity) => dispatch({ type: "setQuantity", id, quantity }),
    []
  );
  const clear = useCallback<CartApi["clear"]>(
    () => dispatch({ type: "clear" }),
    []
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
      add,
      remove,
      setQuantity,
      clear,
    };
  }, [state, add, remove, setQuantity, clear]);

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
