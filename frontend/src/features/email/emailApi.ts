import { apiClient } from "../../shared";

/**
 * Typed client for the AI Email module (API_SPEC §12). Reuses the shared `apiClient` (Bearer + cookies),
 * describing only the single documented operation (FRONTEND_CODING_STANDARDS §8): generate a draft. The
 * API never sends email (BR-034) and there is no get/list/send endpoint, so this client has only generate.
 */

/** AI Request lifecycle (SRS §7.2). `content` is present only when `status === "COMPLETED"`. */
export type AiRequestStatus =
  | "REQUESTED"
  | "IN_PROGRESS"
  | "COMPLETED"
  | "FAILED";

/**
 * AIResponse (API_SPEC §17.5) for an email draft — `type` is always `EMAIL`, `prompt` is the instruction,
 * and `content` is the editable draft (present when `COMPLETED`). The referenced client is context-only and
 * is not part of the response.
 */
export interface EmailDraft {
  id: string;
  type: "EMAIL";
  status: AiRequestStatus;
  documentId: string | null;
  prompt: string | null;
  content: string | null;
  edited: boolean;
  failureReason: string | null;
  createdAt: string;
  updatedAt: string;
}

/** Body for `POST /ai/emails`: the instruction plus optional client/document context. */
export interface GenerateEmailPayload {
  instruction: string;
  clientId?: string;
  documentId?: string;
}

/** API_SPEC §12.1: draft a professional client email from an instruction and optional context. */
export async function generateEmail(
  payload: GenerateEmailPayload,
): Promise<EmailDraft> {
  const { data } = await apiClient.post<EmailDraft>("/ai/emails", payload);
  return data;
}
