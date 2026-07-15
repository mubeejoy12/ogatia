/**
 * Delivery options.
 *
 * Prices are in whole Naira. Timelines are informational — the atelier
 * confirms the exact date once the commission is packed. When shipping
 * partner integration lands, this file becomes a service call; the shape
 * stays identical.
 */

export type DeliveryMethodId =
  | "lagos-standard"
  | "nigeria-nationwide"
  | "international-dhl"
  | "atelier-pickup";

export type DeliveryMethod = {
  id: DeliveryMethodId;
  name: string;
  price: number;
  eta: string;
  description: string;
};

export const deliveryMethods: DeliveryMethod[] = [
  {
    id: "lagos-standard",
    name: "Lagos delivery",
    price: 8000,
    eta: "3 – 5 business days",
    description:
      "Insured door-to-door delivery within Lagos, hand-carried by the atelier's courier partner.",
  },
  {
    id: "nigeria-nationwide",
    name: "Nigeria nationwide",
    price: 15000,
    eta: "5 – 7 business days",
    description: "Insured delivery to any city in Nigeria.",
  },
  {
    id: "international-dhl",
    name: "International · DHL insured",
    price: 45000,
    eta: "7 – 14 business days",
    description:
      "Fully insured worldwide shipping via DHL. Duties and taxes may apply on arrival.",
  },
  {
    id: "atelier-pickup",
    name: "Atelier pickup",
    price: 0,
    eta: "Ready when your commission is",
    description:
      "Collect from the Eazi Cut atelier in Victoria Island, Lagos, by private appointment.",
  },
];

export const getDeliveryMethod = (
  id: DeliveryMethodId | null
): DeliveryMethod | null =>
  id ? deliveryMethods.find((m) => m.id === id) ?? null : null;
