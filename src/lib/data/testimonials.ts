export type Testimonial = {
  quote: string;
  role: string;
  city: string;
};

/**
 * Client voices are attributed by role + city only.
 *
 * Luxury houses do not name individual clients in marketing copy — it
 * both protects patron privacy and reads as more considered. Replace or
 * augment with named, consented quotes only when the client has signed
 * off in writing.
 */
export const testimonials: Testimonial[] = [
  {
    quote:
      "The fit was immaculate. I have worn bespoke from London and Milan — Eazi Cut sits comfortably in that conversation.",
    role: "Managing Partner",
    city: "Lagos",
  },
  {
    quote:
      "They tailored an entire wedding party spread across three continents. Every measurement, every fabric, handled with quiet precision.",
    role: "Groom",
    city: "London",
  },
  {
    quote:
      "The first house in Lagos that treats menswear as craft rather than commerce. An atelier in the truest sense.",
    role: "Creative Director",
    city: "Abuja",
  },
];
