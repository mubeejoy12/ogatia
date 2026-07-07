import { Hero } from "@/components/sections/Hero";
import { FeaturedCollections } from "@/components/sections/FeaturedCollections";
import { WhyChooseEaziCut } from "@/components/sections/WhyChooseEaziCut";
import { TailoringProcess } from "@/components/sections/TailoringProcess";
import { Testimonials } from "@/components/sections/Testimonials";
import { CTASection } from "@/components/sections/CTASection";

export default function HomePage() {
  return (
    <>
      <Hero />
      <FeaturedCollections limit={4} />
      <WhyChooseEaziCut />
      <TailoringProcess />
      <Testimonials />
      <CTASection />
    </>
  );
}
