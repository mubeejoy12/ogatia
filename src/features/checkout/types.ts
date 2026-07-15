/**
 * Countries the atelier ships to at launch — Nigeria plus the primary
 * diaspora destinations. "Other" collects the long-tail; the atelier
 * confirms delivery availability by hand for those orders.
 */
export const shippingCountries = [
  "Nigeria",
  "United Kingdom",
  "United States",
  "Canada",
  "Ireland",
  "France",
  "Germany",
  "Netherlands",
  "United Arab Emirates",
  "Saudi Arabia",
  "Qatar",
  "South Africa",
  "Ghana",
  "Kenya",
  "Australia",
  "Other",
] as const;

export type ShippingCountry = (typeof shippingCountries)[number];

export type ShippingAddress = {
  fullName: string;
  email: string;
  phone: string;
  addressLine1: string;
  addressLine2: string;
  city: string;
  region: string;
  postalCode: string;
  country: ShippingCountry;
};

/**
 * A blank address — used as the initial form state.
 */
export const emptyAddress: ShippingAddress = {
  fullName: "",
  email: "",
  phone: "",
  addressLine1: "",
  addressLine2: "",
  city: "",
  region: "",
  postalCode: "",
  country: "Nigeria",
};

/**
 * Per-field validation errors. Missing key = no error for that field.
 */
export type AddressErrors = Partial<Record<keyof ShippingAddress, string>>;
