import * as React from "react";
import { cn } from "@/lib/utils";

export const Select = React.forwardRef<
  HTMLSelectElement,
  React.SelectHTMLAttributes<HTMLSelectElement>
>(({ className, children, ...props }, ref) => (
  <select
    ref={ref}
    className={cn(
      "w-full bg-transparent border-b border-ink/30 py-3 text-ink",
      "focus:border-ink focus:outline-none transition-colors duration-300",
      "disabled:opacity-50 disabled:cursor-not-allowed",
      className
    )}
    {...props}
  >
    {children}
  </select>
));
Select.displayName = "Select";
