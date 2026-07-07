export type Testimonial = {
  quote: string;
  name: string;
  role: string;
  city: string;
};

export const testimonials: Testimonial[] = [
  {
    quote:
      "The fit was immaculate. I have worn bespoke from London and Milan — Eazi Cut sits comfortably in that conversation.",
    name: "Adebayo O.",
    role: "Managing Partner",
    city: "Lagos",
  },
  {
    quote:
      "They tailored my entire wedding party from three continents. Every measurement, every fabric, handled with quiet precision.",
    name: "Chinedu E.",
    role: "Groom",
    city: "London",
  },
  {
    quote:
      "An atelier in the truest sense. The first house in Lagos that treats menswear as craft, not commerce.",
    name: "Tunde A.",
    role: "Creative Director",
    city: "Abuja",
  },
];
