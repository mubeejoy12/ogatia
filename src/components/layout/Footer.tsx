import Link from "next/link";
import { nav, site } from "@/lib/site";
import { Instagram, Twitter } from "lucide-react";

export function Footer() {
  return (
    <footer className="bg-ink text-ivory">
      <div className="container py-24">
        <div className="grid grid-cols-1 md:grid-cols-12 gap-12 md:gap-8">
          <div className="md:col-span-5">
            <Link
              href="/"
              className="font-display text-3xl tracking-tightest"
            >
              Eazi<span className="text-gold">.</span>Cut
            </Link>
            <p className="mt-6 max-w-sm text-ivory/70 leading-relaxed">
              {site.description}
            </p>
            <p className="mt-10 eyebrow text-ivory/40">Atelier</p>
            <p className="mt-2 text-ivory/80">{site.address}</p>
          </div>

          <div className="md:col-span-3">
            <p className="eyebrow text-ivory/40">Explore</p>
            <ul className="mt-4 space-y-3">
              {nav.map((item) => (
                <li key={item.href}>
                  <Link
                    href={item.href}
                    className="text-ivory/80 hover:text-gold transition-colors duration-300"
                  >
                    {item.label}
                  </Link>
                </li>
              ))}
            </ul>
          </div>

          <div className="md:col-span-4">
            <p className="eyebrow text-ivory/40">Contact</p>
            <ul className="mt-4 space-y-3 text-ivory/80">
              <li>
                <a
                  href={`mailto:${site.email}`}
                  className="hover:text-gold transition-colors"
                >
                  {site.email}
                </a>
              </li>
              <li>
                <a
                  href={`tel:${site.phone.replace(/\s/g, "")}`}
                  className="hover:text-gold transition-colors"
                >
                  {site.phone}
                </a>
              </li>
            </ul>
            <div className="mt-8 flex gap-4">
              <a
                href={site.social.instagram}
                aria-label="Instagram"
                className="p-2 border border-ivory/20 hover:border-gold hover:text-gold transition-colors"
              >
                <Instagram size={16} />
              </a>
              <a
                href={site.social.twitter}
                aria-label="Twitter"
                className="p-2 border border-ivory/20 hover:border-gold hover:text-gold transition-colors"
              >
                <Twitter size={16} />
              </a>
            </div>
          </div>
        </div>

        <div className="mt-20 pt-8 border-t border-ivory/10 flex flex-col md:flex-row justify-between items-start md:items-center gap-4 text-xs text-ivory/40 uppercase tracking-widest">
          <p>© {new Date().getFullYear()} Eazi Cut. All rights reserved.</p>
          <p>Tailored in Lagos. Shipped worldwide.</p>
        </div>
      </div>
    </footer>
  );
}
