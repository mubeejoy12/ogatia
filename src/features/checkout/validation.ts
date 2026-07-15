import { shippingCountries, type AddressErrors, type ShippingAddress } from "./types";

// Reasonable, permissive checks — not RFC 5322 pedantry. Any address
// serious enough to warrant a bespoke commission gets confirmed by the
// atelier team before shipping anyway.
const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/;
const PHONE_RE = /^[+\d\s().-]{7,}$/;

/**
 * Validate the entire address. Returns an object of field → message for
 * any fields that failed. Empty object means the address is valid.
 */
export function validateAddress(address: ShippingAddress): AddressErrors {
  const errors: AddressErrors = {};

  if (address.fullName.trim().length < 2) {
    errors.fullName = "Please enter your full name.";
  }

  if (!EMAIL_RE.test(address.email.trim())) {
    errors.email = "Please enter a valid email.";
  }

  if (!PHONE_RE.test(address.phone.trim())) {
    errors.phone = "Please enter a valid phone number, including country code.";
  }

  if (address.addressLine1.trim().length < 5) {
    errors.addressLine1 = "Please enter your street address.";
  }

  if (address.city.trim().length < 2) {
    errors.city = "Please enter your city.";
  }

  if (address.region.trim().length < 2) {
    errors.region = "Please enter your state or region.";
  }

  if (!shippingCountries.includes(address.country)) {
    errors.country = "Please choose a country from the list.";
  }

  // Nigeria doesn't require a postal code — leave it optional for every country.

  return errors;
}

export const isAddressValid = (address: ShippingAddress): boolean =>
  Object.keys(validateAddress(address)).length === 0;
