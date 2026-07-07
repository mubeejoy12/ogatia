"use client";

import { useState } from "react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import { Select } from "@/components/ui/select";
import { Label } from "@/components/ui/label";
import { useContactForm } from "@/hooks/useContactForm";
import type { ContactService } from "@/types/contact";

const services: ContactService[] = [
  "Bespoke Suiting",
  "Wedding Commission",
  "Ready-to-Wear",
  "Corporate Native Wear",
  "Other",
];

export function ContactForm() {
  const { submit, status } = useContactForm();
  const [error, setError] = useState<string | null>(null);

  return (
    <form
      aria-label="Contact form"
      className="space-y-8"
      onSubmit={async (e) => {
        e.preventDefault();
        setError(null);
        const data = new FormData(e.currentTarget);
        const result = await submit(data);
        if (!result.ok) setError(result.message);
      }}
    >
      <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
        <Field label="Full name" name="name" required />
        <Field label="Email" name="email" type="email" required />
        <Field label="Phone" name="phone" type="tel" />
        <Field label="City" name="city" />
      </div>

      <div>
        <Label htmlFor="service" className="mb-3">
          Service of interest
        </Label>
        <Select id="service" name="service" defaultValue="">
          <option value="" disabled>
            Select a service
          </option>
          {services.map((s) => (
            <option key={s} value={s}>
              {s}
            </option>
          ))}
        </Select>
      </div>

      <div>
        <Label htmlFor="message" className="mb-3">
          Tell us about your commission
        </Label>
        <Textarea
          id="message"
          name="message"
          rows={6}
          placeholder="The occasion, the timeline, references you love…"
        />
      </div>

      <div className="pt-4">
        <Button
          type="submit"
          variant="primary"
          size="lg"
          disabled={status === "submitting"}
        >
          {status === "submitting" ? "Sending…" : "Send Enquiry"}
        </Button>
        <p className="mt-4 text-xs text-stone-500 max-w-md">
          By submitting, you agree to be contacted by the Eazi Cut atelier
          regarding your commission.
        </p>
        {status === "success" && (
          <p
            role="status"
            className="mt-6 eyebrow text-gold-deep"
          >
            Thank you. A client director will be in touch within one business day.
          </p>
        )}
        {error && (
          <p role="alert" className="mt-6 eyebrow text-red-700">
            {error}
          </p>
        )}
      </div>
    </form>
  );
}

function Field({
  label,
  name,
  type = "text",
  required = false,
}: {
  label: string;
  name: string;
  type?: string;
  required?: boolean;
}) {
  return (
    <div>
      <Label htmlFor={name} className="mb-3">
        {label}
        {required && <span className="text-gold ml-1">*</span>}
      </Label>
      <Input id={name} name={name} type={type} required={required} />
    </div>
  );
}
