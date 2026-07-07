"use client";

import { useState } from "react";
import { submitContactEnquiry } from "@/services/contact";
import type {
  ContactEnquiry,
  ContactService,
  ContactSubmitResult,
} from "@/types/contact";

type Status = "idle" | "submitting" | "success" | "error";

export function useContactForm() {
  const [status, setStatus] = useState<Status>("idle");

  async function submit(data: FormData): Promise<ContactSubmitResult> {
    setStatus("submitting");
    const enquiry: ContactEnquiry = {
      name: String(data.get("name") ?? ""),
      email: String(data.get("email") ?? ""),
      phone: (data.get("phone") as string) || undefined,
      city: (data.get("city") as string) || undefined,
      service: (data.get("service") as ContactService) || undefined,
      message: (data.get("message") as string) || undefined,
    };
    const result = await submitContactEnquiry(enquiry);
    setStatus(result.ok ? "success" : "error");
    return result;
  }

  return { submit, status };
}
