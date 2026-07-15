"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { useEffect, useState } from "react";
import { Menu, X } from "lucide-react";
import { cn } from "@/lib/utils";
import { nav, site } from "@/lib/site";
import { Button } from "@/components/ui/button";
import { CartBadge } from "@/features/cart/CartBadge";

export function Navbar() {
  const pathname = usePathname();
  const [scrolled, setScrolled] = useState(false);
  const [open, setOpen] = useState(false);

  useEffect(() => {
    const onScroll = () => setScrolled(window.scrollY > 24);
    onScroll();
    window.addEventListener("scroll", onScroll, { passive: true });
    return () => window.removeEventListener("scroll", onScroll);
  }, []);

  useEffect(() => {
    setOpen(false);
  }, [pathname]);

  return (
    <header
      className={cn(
        "fixed inset-x-0 top-0 z-50 transition-all duration-500 ease-editorial",
        scrolled || open
          ? "bg-ivory/95 backdrop-blur border-b border-ink/10"
          : "bg-transparent border-b border-transparent"
      )}
    >
      <div className="container flex h-20 items-center justify-between">
        <Link
          href="/"
          aria-label={`${site.name} — Home`}
          className="font-display text-2xl tracking-tightest text-ink"
        >
          Eazi<span className="text-gold">.</span>Cut
        </Link>

        <nav
          aria-label="Primary"
          className="hidden md:flex items-center gap-10"
        >
          {nav.map((item) => {
            const active =
              item.href === "/"
                ? pathname === "/"
                : pathname.startsWith(item.href);
            return (
              <Link
                key={item.href}
                href={item.href}
                className={cn(
                  "uppercase tracking-widest text-[11px] font-sans transition-colors duration-300",
                  active ? "text-ink" : "text-ink/60 hover:text-ink"
                )}
                aria-current={active ? "page" : undefined}
              >
                {item.label}
              </Link>
            );
          })}
        </nav>

        <div className="hidden md:flex items-center gap-4">
          <CartBadge />
          <Button asChild size="sm" variant="primary">
            <Link href="/contact">Book a Fitting</Link>
          </Button>
        </div>

        <div className="md:hidden flex items-center gap-2">
          <CartBadge />
          <button
            type="button"
            className="p-2 -mr-2 text-ink"
            aria-label={open ? "Close menu" : "Open menu"}
            aria-expanded={open}
            onClick={() => setOpen((v) => !v)}
          >
            {open ? <X size={22} /> : <Menu size={22} />}
          </button>
        </div>
      </div>

      {open && (
        <div className="md:hidden border-t border-ink/10 bg-ivory">
          <nav
            aria-label="Mobile"
            className="container flex flex-col py-6 gap-4"
          >
            {nav.map((item) => (
              <Link
                key={item.href}
                href={item.href}
                className="uppercase tracking-widest text-xs py-2 text-ink"
              >
                {item.label}
              </Link>
            ))}
            <Button asChild size="sm" className="mt-2 self-start">
              <Link href="/contact">Book a Fitting</Link>
            </Button>
          </nav>
        </div>
      )}
    </header>
  );
}
