import { apiClient } from "../../shared";

/**
 * Typed client for the AI Summary module (API_SPEC §10). Reuses the shared `apiClient` (Bearer +
 * cookies), describing only the three documented summary operations (FRONTEND_CODING_STANDARDS §8).
 * This slice implements AI Summary only — there are deliberately no chat, email, or report calls.
 */

/** AI Request lifecycle (SRS §7.2). `content` is present only when `status === "COMPLETED"`. */
export type AiRequestStatus =
  | "REQUESTED"
  | "IN_PROGRESS"
  | "COMPLETED"
  | "FAILED";

/** Only SUMMARY is implemented in this slice. */
export type AiRequestType = "SUMMARY";

/** AIResponse (API_SPEC §17.5) — mirrors AIRequest + its optional editable AIOutput. */
export interface AiResponse {
  id: string;
  type: AiRequestType;
  status: AiRequestStatus;
  documentId: string | null;
  prompt: string | null;
  content: string | null;
  edited: boolean;
  failureReason: string | null;
  createdAt: string;
  updatedAt: string;
}

/**
 * API_SPEC §10.1: generate (or, with `regenerate`, re-generate) the document's summary. The body is
 * sent only when forcing a regeneration; otherwise an existing summary is returned as-is.
 */
export async function generateSummary(
  documentId: string,
  regenerate = false,
): Promise<AiResponse> {
  const { data } = await apiClient.post<AiResponse>(
    `/documents/${documentId}/summary`,
    regenerate ? { regenerate: true } : undefined,
  );
  return data;
}

/** API_SPEC §10.2: the saved summary and its status (also the async poll target). */
export async function getSummary(documentId: string): Promise<AiResponse> {
  const { data } = await apiClient.get<AiResponse>(
    `/documents/${documentId}/summary`,
  );
  return data;
}

/** API_SPEC §10.3: persist the user's edit to the summary content (human-in-the-loop, BR-031). */
export async function editSummary(
  documentId: string,
  content: string,
): Promise<AiResponse> {
  const { data } = await apiClient.patch<AiResponse>(
    `/documents/${documentId}/summary`,
    { content },
  );
  return data;
}
