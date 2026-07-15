import Link from "next/link";
import { Button } from "@/components/ui/button";

/**
 * Rendered when a visitor lands on /checkout without any items in the bag.
 * A checkout with no order to buy would be nonsense — send them back to
 * the shop with a considered, in-brand explanation.
 */
export function CheckoutEmpty() {
  return (
    <div className="mt-16 md:mt-20 text-center max-w-lg mx-auto">
      <p className="eyebrow">
        <span className="rule inline-block align-middle mr-3" />
        Checkout
        <span className="rule inline-block align-middle ml-3" />
      </p>
      <h2 className="mt-8 font-display text-4xl md:text-5xl leading-[1.05] tracking-tightest">
        There is nothing to checkout yet.
      </h2>
      <p className="mt-6 text-stone-700 leading-relaxed">
        Add a piece to your bag first, or arrange a private consultation to
        commission bespoke.
      </p>
      <div className="mt-10 flex flex-col sm:flex-row gap-4 justify-center">
        <Button asChild variant="primary" size="lg">
          <Link href="/shop">Return to Shop</Link>
        </Button>
        <Button asChild variant="secondary" size="lg">
          <Link href="/contact">Speak With Us</Link>
        </Button>
      </div>
    </div>
  );
}
