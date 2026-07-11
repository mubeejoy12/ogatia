/**
 * Format a whole-Naira amount for luxury retail display.
 *
 * Uses the browser/Node `Intl.NumberFormat` for locale-correct grouping,
 * then prefixes the ₦ symbol. Kept in one file so pricing style is
 * consistent everywhere (Shop grid, Product Details, Cart, receipts).
 *
 * @example formatNaira(450000)   // "₦450,000"
 * @example formatNaira(1250000)  // "₦1,250,000"
 */
export function formatNaira(amount: number): string {
  const grouped = new Intl.NumberFormat("en-NG", {
    maximumFractionDigits: 0,
  }).format(Math.round(amount));
  return `₦${grouped}`;
}
