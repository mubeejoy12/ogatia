import type { Metadata } from "next";
import Image from "next/image";
import { Mail, MapPin, Phone } from "lucide-react";
import { PageHeader } from "@/components/sections/PageHeader";
import { Reveal } from "@/components/motion/Reveal";
import { ContactForm } from "@/features/contact/ContactForm";
import { site } from "@/lib/site";

export const metadata: Metadata = {
  title: "Contact",
  description:
    "Book a fitting at the Eazi Cut Lagos atelier or arrange a private video consultation from anywhere in the world.",
};

export default function ContactPage() {
  return (
    <>
      <PageHeader
        eyebrow="Contact"
        title="Begin a conversation."
        intro="Tell us about your commission. A senior client director will respond within one business day."
      />

      <section className="pb-24 md:pb-32 bg-ivory">
        <div className="container grid grid-cols-1 lg:grid-cols-12 gap-12 lg:gap-16">
          <Reveal className="lg:col-span-7">
            <ContactForm />
          </Reveal>

          <Reveal className="lg:col-span-5 lg:pl-10 lg:border-l lg:border-ink/10">
            <p className="eyebrow">
              <span className="rule inline-block align-middle mr-3" />
              The Atelier
            </p>
            <div className="relative aspect-[4/5] mt-6 overflow-hidden">
              <Image
                src="https://images.unsplash.com/photo-1520975916090-3105956dac38?auto=format&fit=crop&w=1200&q=80"
                alt="Inside the Eazi Cut Lagos atelier"
                fill
                sizes="(min-width: 1024px) 35vw, 100vw"
                className="object-cover"
              />
            </div>

            <ul className="mt-10 space-y-6 text-ink">
              <li className="flex gap-4">
                <MapPin size={18} className="mt-1 shrink-0 text-gold" />
                <div>
                  <p className="eyebrow text-stone-500">Visit</p>
                  <p className="mt-1">{site.address}</p>
                  <p className="text-stone-500 text-sm mt-1">
                    By appointment, Tue – Sat
                  </p>
                </div>
              </li>
              <li className="flex gap-4">
                <Mail size={18} className="mt-1 shrink-0 text-gold" />
                <div>
                  <p className="eyebrow text-stone-500">Write</p>
                  <a
                    href={`mailto:${site.email}`}
                    className="mt-1 inline-block hover:text-gold transition-colors"
                  >
                    {site.email}
                  </a>
                </div>
              </li>
              <li className="flex gap-4">
                <Phone size={18} className="mt-1 shrink-0 text-gold" />
                <div>
                  <p className="eyebrow text-stone-500">Call</p>
                  <a
                    href={`tel:${site.phone.replace(/\s/g, "")}`}
                    className="mt-1 inline-block hover:text-gold transition-colors"
                  >
                    {site.phone}
                  </a>
                </div>
              </li>
            </ul>
          </Reveal>
        </div>
      </section>
    </>
  );
}
