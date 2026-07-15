import Link from "next/link";
import { Button } from "@/components/ui/button";

/**
 * Rendered when the cart is empty. Voice is understated — no exclamations,
 * no discount teasers. Two routes forward: back to the shop, or into the
 * atelier for a private conversation.
 */
export function CartEmpty() {
  return (
    <div className="mt-20 md:mt-24 text-center max-w-lg mx-auto">
      <p className="eyebrow">
        <span className="rule inline-block align-middle mr-3" />
        Your bag
        <span className="rule inline-block align-middle ml-3" />
      </p>
      <h2 className="mt-8 font-display text-4xl md:text-5xl leading-[1.05] tracking-tightest">
        The bag is empty for now.
      </h2>
      <p className="mt-6 text-stone-700 leading-relaxed">
        Return to the shop, or arrange a private consultation with the atelier
        to commission a piece from scratch.
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
