export const site = {
  name: "Eazi Cut",
  tagline: "Luxury menswear, tailored without friction.",
  description:
    "Eazi Cut is a luxury African menswear house — bespoke tailoring, premium ready-to-wear, and wedding collections crafted for the modern gentleman.",
  url: "https://eazicut.com",
  email: "atelier@eazicut.com",
  phone: "+234 800 000 0000",
  address: "The Atelier, Victoria Island, Lagos",
  social: {
    instagram: "https://instagram.com/eazicut",
    twitter: "https://twitter.com/eazicut",
  },
} as const;

export const nav = [
  { href: "/", label: "Home" },
  { href: "/collections", label: "Collections" },
  { href: "/shop", label: "Shop" },
  { href: "/lookbook", label: "Lookbook" },
  { href: "/about", label: "Atelier" },
  { href: "/contact", label: "Contact" },
] as const;
