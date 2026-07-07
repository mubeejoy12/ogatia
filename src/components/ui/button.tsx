import * as React from "react";
import { Slot } from "@radix-ui/react-slot";
import { cva, type VariantProps } from "class-variance-authority";
import { cn } from "@/lib/utils";

const buttonVariants = cva(
  "inline-flex items-center justify-center gap-2 font-sans uppercase tracking-widest text-[11px] transition-all duration-500 ease-editorial disabled:opacity-40 disabled:pointer-events-none whitespace-nowrap",
  {
    variants: {
      variant: {
        primary:
          "bg-ink text-ivory hover:bg-gold-deep px-8 py-4 border border-ink",
        secondary:
          "bg-transparent text-ink border border-ink hover:bg-ink hover:text-ivory px-8 py-4",
        ghost:
          "bg-transparent text-ink hover:text-gold-deep px-2 py-2 border-b border-transparent hover:border-current",
        gold:
          "bg-gold text-ivory hover:bg-gold-deep px-8 py-4 border border-gold",
        outlineIvory:
          "bg-transparent text-ivory border border-ivory hover:bg-ivory hover:text-ink px-8 py-4",
      },
      size: {
        default: "",
        sm: "px-5 py-3 text-[10px]",
        lg: "px-10 py-5 text-xs",
      },
    },
    defaultVariants: { variant: "primary", size: "default" },
  }
);

export interface ButtonProps
  extends React.ButtonHTMLAttributes<HTMLButtonElement>,
    VariantProps<typeof buttonVariants> {
  asChild?: boolean;
}

export const Button = React.forwardRef<HTMLButtonElement, ButtonProps>(
  ({ className, variant, size, asChild = false, ...props }, ref) => {
    const Comp = asChild ? Slot : "button";
    return (
      <Comp
        ref={ref}
        className={cn(buttonVariants({ variant, size }), className)}
        {...props}
      />
    );
  }
);
Button.displayName = "Button";

export { buttonVariants };
