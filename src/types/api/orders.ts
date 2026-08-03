/**
 * Mirror of the backend order DTOs
 * ({@code com.eazicut.api.orders.dto.*}).
 *
 * <p>Field names + shapes match the JSON on the wire verbatim — do
 * not rename without updating the backend at the same time.
 */

export type ApiOrderStatus =
  | "PENDING_PAYMENT"
  | "PAID"
  | "FULFILLING"
  | "SHIPPED"
  | "DELIVERED"
  | "CANCELLED"
  | "REFUNDED";

export type ApiShippingAddress = {
  fullName: string;
  email: string;
  phone: string;
  addressLine1: string;
  addressLine2: string | null;
  city: string;
  region: string | null;
  postalCode: string | null;
  country: string;
};

export type ApiOrderItem = {
  id: string;
  productId: string;
  productSlug: string;
  productName: string;
  productImageUrl: string | null;
  size: string;
  quantity: number;
  unitPrice: string;   // BigDecimal as string
  currency: string;
  lineTotal: string;
};

export type ApiOrder = {
  id: string;
  reference: string;
  status: ApiOrderStatus;
  currency: string;
  subtotal: string;
  shippingCost: string;
  total: string;
  deliveryMethodId: string;
  deliveryMethodName: string;
  shippingAddress: ApiShippingAddress;
  items: ApiOrderItem[];
  placedAt: string;
  createdAt: string;
  updatedAt: string;
};

// ---------------------------------------------------------------------------
// Request
// ---------------------------------------------------------------------------

export type ApiCreateShippingAddressRequest = {
  fullName: string;
  email: string;
  phone: string;
  addressLine1: string;
  addressLine2?: string;
  city: string;
  region?: string;
  postalCode?: string;
  country: string;
};

export type ApiCreateOrderRequest = {
  deliveryMethodId: string;
  shippingAddress: ApiCreateShippingAddressRequest;
  /** BigDecimal as string. Compared server-side against a live-price
   *  recompute; a mismatch → 409 price_mismatch (see backend). */
  expectedTotal: string;
};
