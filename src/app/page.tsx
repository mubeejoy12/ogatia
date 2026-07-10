import type { Metadata } from "next";
import { Hero } from "@/components/sections/Hero";
import { BrandStatement } from "@/components/sections/BrandStatement";
import { FeaturedCollections } from "@/components/sections/FeaturedCollections";
import { WhyChooseEaziCut } from "@/components/sections/WhyChooseEaziCut";
import { TailoringProcess } from "@/components/sections/TailoringProcess";
import { Testimonials } from "@/components/sections/Testimonials";
import { CTASection } from "@/components/sections/CTASection";
import { site } from "@/lib/site";

export const metadata: Metadata = {
  title: `${site.name} — ${site.tagline}`,
  description: site.description,
  alternates: { canonical: site.url },
  openGraph: {
    title: `${site.name} — ${site.tagline}`,
    description: site.description,
    url: site.url,
    type: "website",
  },
};

/**
 * JSON-LD Organization + LocalBusiness schema.
 *
 * Improves the way Eazi Cut appears in Google's Knowledge Panel and social
 * preview cards. Kept in-page so page-specific facts stay adjacent to the
 * content that expresses them; global facts (name, url, address) come from
 * the single-source-of-truth `site` config.
 */
const organizationSchema = {
  "@context": "https://schema.org",
  "@type": ["Organization", "LocalBusiness"],
  name: site.name,
  url: site.url,
  email: site.email,
  telephone: site.phone,
  description: site.description,
  address: {
    "@type": "PostalAddress",
    streetAddress: site.address,
    addressLocality: "Lagos",
    addressCountry: "NG",
  },
  sameAs: [site.social.instagram, site.social.twitter],
  makesOffer: [
    { "@type": "Offer", name: "Bespoke Tailoring" },
    { "@type": "Offer", name: "Ready-to-Wear" },
    { "@type": "Offer", name: "Wedding Commissions" },
    { "@type": "Offer", name: "Heritage & Native Wear" },
  ],
};

export default function HomePage() {
  return (
    <>
      <script
        type="application/ld+json"
        // eslint-disable-next-line react/no-danger
        dangerouslySetInnerHTML={{ __html: JSON.stringify(organizationSchema) }}
      />
      <Hero />
      <BrandStatement />
      <FeaturedCollections limit={4} />
      <WhyChooseEaziCut />
      <TailoringProcess />
      <Testimonials />
      <CTASection />
    </>
  );
}
