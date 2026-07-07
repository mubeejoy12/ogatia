export type ContactService =
  | "Bespoke Suiting"
  | "Wedding Commission"
  | "Ready-to-Wear"
  | "Corporate Native Wear"
  | "Other";

export interface ContactEnquiry {
  name: string;
  email: string;
  phone?: string;
  city?: string;
  service?: ContactService;
  message?: string;
}

export type ContactSubmitResult =
  | { ok: true }
  | { ok: false; message: string };
