import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import type { Page } from "../../shared";
import type { ChatExchange, ChatHistoryQuery } from "./chatApi";
import { askQuestion, getChatHistory } from "./chatApi";

/**
 * Server-state hooks for AI Chat (FRONTEND_CODING_STANDARDS §6, ADR-007). The chat thread lives in React
 * Query, never a global store. Asking a question invalidates the thread so the new exchange appears.
 */

export const chatKeys = {
  all: ["chat"] as const,
  thread: (documentId: string, query: ChatHistoryQuery) =>
    ["chat", "thread", documentId, query] as const,
};

/**
 * API_SPEC §11.2: the document's chat thread. `enabled` lets a caller (the document panel) suppress the
 * fetch until the document is `READY` — a non-`READY` document has no thread.
 */
export function useChatHistory(
  documentId: string,
  query: ChatHistoryQuery = {},
  options: { enabled?: boolean } = {},
) {
  return useQuery<Page<ChatExchange>>({
    queryKey: chatKeys.thread(documentId, query),
    queryFn: () => getChatHistory(documentId, query),
    enabled: (options.enabled ?? true) && Boolean(documentId),
  });
}

/** API_SPEC §11.1: ask a question about the document; invalidates the thread so it re-fetches. */
export function useAskQuestion(documentId: string) {
  const queryClient = useQueryClient();
  return useMutation<ChatExchange, unknown, string>({
    mutationFn: (question) => askQuestion(documentId, question),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: chatKeys.all });
    },
  });
}
