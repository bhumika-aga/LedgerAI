import type { Page } from "../../shared";
import { apiClient } from "../../shared";

/**
 * Typed client for the AI Chat module (API_SPEC §11). Reuses the shared `apiClient` (Bearer + cookies) and
 * shared `Page`, describing only the two documented operations (FRONTEND_CODING_STANDARDS §8). MVP chat is
 * document-scoped — both operations are addressed under the owning document; there is no conversation
 * resource.
 */

/** AI Request lifecycle (SRS §7.2). `content` is present only when `status === "COMPLETED"`. */
export type AiRequestStatus =
  | "REQUESTED"
  | "IN_PROGRESS"
  | "COMPLETED"
  | "FAILED";

/**
 * AIResponse (API_SPEC §17.5) for a chat exchange — `type` is always `CHAT`, `prompt` is the question the
 * user asked, and `content` is the grounded answer (present when `COMPLETED`).
 */
export interface ChatExchange {
  id: string;
  type: "CHAT";
  status: AiRequestStatus;
  documentId: string | null;
  prompt: string | null;
  content: string | null;
  edited: boolean;
  failureReason: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface ChatHistoryQuery {
  page?: number;
  size?: number;
}

/** API_SPEC §11.1: ask a grounded question about a `READY` document; returns the resulting exchange. */
export async function askQuestion(
  documentId: string,
  question: string,
): Promise<ChatExchange> {
  const { data } = await apiClient.post<ChatExchange>(
    `/documents/${documentId}/chat`,
    { question },
  );
  return data;
}

/** API_SPEC §11.2: the document's chat thread, chronological (oldest first), paged. */
export async function getChatHistory(
  documentId: string,
  query: ChatHistoryQuery = {},
): Promise<Page<ChatExchange>> {
  const { data } = await apiClient.get<Page<ChatExchange>>(
    `/documents/${documentId}/chat`,
    { params: query },
  );
  return data;
}
