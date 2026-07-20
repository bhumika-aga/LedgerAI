import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import type { AiRequestStatus, AiResponse } from "./aiApi";
import { editSummary, generateSummary, getSummary } from "./aiApi";

/**
 * Server-state hooks for AI summaries (FRONTEND_CODING_STANDARDS §6, ADR-007). Summary data lives in
 * React Query, never a global store. Generate and edit write the returned resource back into the
 * summary cache so the panel reflects the change immediately.
 */

/** Lifecycle states where generation is still in flight, so the summary should keep polling. */
const IN_FLIGHT_STATUSES: ReadonlySet<AiRequestStatus> = new Set([
  "REQUESTED",
  "IN_PROGRESS",
]);

export const summaryKeys = {
  all: ["summary"] as const,
  detail: (documentId: string) => ["summary", "detail", documentId] as const,
};

/**
 * API_SPEC §10.2 / §2.11 — the saved summary and its status, and the async poll target. A `404` (no
 * summary yet) is left as the query error for the panel to render as an empty "generate" state rather
 * than a failure. While generation is in flight ({@code REQUESTED}/{@code IN_PROGRESS}) it re-fetches on
 * an interval; a terminal status ({@code COMPLETED}/{@code FAILED}) stops polling. This honors the
 * async-ready contract even though the MVP generates synchronously (ADR-013).
 */
export function useSummary(documentId: string) {
  return useQuery<AiResponse>({
    queryKey: summaryKeys.detail(documentId),
    queryFn: () => getSummary(documentId),
    enabled: Boolean(documentId),
    retry: false,
    refetchInterval: (query) => {
      const status = query.state.data?.status;
      return status && IN_FLIGHT_STATUSES.has(status) ? 2000 : false;
    },
  });
}

/** API_SPEC §10.1: generate (or regenerate) the summary; seeds the summary cache from the result. */
export function useGenerateSummary(documentId: string) {
  const queryClient = useQueryClient();
  return useMutation<AiResponse, unknown, boolean | void>({
    mutationFn: (regenerate) =>
      generateSummary(documentId, Boolean(regenerate)),
    onSuccess: (result) => {
      queryClient.setQueryData(summaryKeys.detail(documentId), result);
    },
  });
}

/** API_SPEC §10.3: persist a user edit; updates the summary cache from the result. */
export function useEditSummary(documentId: string) {
  const queryClient = useQueryClient();
  return useMutation<AiResponse, unknown, string>({
    mutationFn: (content) => editSummary(documentId, content),
    onSuccess: (result) => {
      queryClient.setQueryData(summaryKeys.detail(documentId), result);
    },
  });
}
