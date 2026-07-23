import { useMutation } from "@tanstack/react-query";
import type { EmailDraft, GenerateEmailPayload } from "./emailApi";
import { generateEmail } from "./emailApi";

/**
 * Server-state hook for AI Email (FRONTEND_CODING_STANDARDS §6, ADR-007). Email generation is a one-shot
 * mutation — there is no server-side list/thread to cache (the API only generates, never sends or stores a
 * retrievable draft resource), so nothing is invalidated; the page holds the returned draft locally.
 */
export function useGenerateEmail() {
  return useMutation<EmailDraft, unknown, GenerateEmailPayload>({
    mutationFn: (payload) => generateEmail(payload),
  });
}
