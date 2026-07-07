import type { ContactEnquiry, ContactSubmitResult } from "@/types/contact";

export async function submitContactEnquiry(
  enquiry: ContactEnquiry
): Promise<ContactSubmitResult> {
  if (!enquiry.name || !enquiry.email) {
    return { ok: false, message: "Name and email are required." };
  }

  await new Promise((r) => setTimeout(r, 600));
  return { ok: true };
}
