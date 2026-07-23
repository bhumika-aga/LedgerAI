import { keepPreviousData, useQuery } from "@tanstack/react-query";

import type { Page } from "../../shared";
import type { SearchResult } from "./searchApi";
import { searchDocuments } from "./searchApi";

/**
 * Server-state hook for Global Search (FRONTEND_CODING_STANDARDS §6, ADR-007). Results live in React
 * Query, never a global store. The query runs only once a non-blank keyword has been submitted (an empty
 * `q` is not a request — the page shows a prompt instead); `keepPreviousData` holds the current results
 * while the next page loads. Search is read-only, so there are no mutation hooks.
 */

export const searchKeys = {
  all: ["search"] as const,
  query: (q: string, page: number) => ["search", q, page] as const,
};

export function useSearch(q: string, page: number) {
  const trimmed = q.trim();
  return useQuery<Page<SearchResult>>({
    queryKey: searchKeys.query(trimmed, page),
    queryFn: () => searchDocuments({ q: trimmed, page }),
    enabled: trimmed.length > 0,
    placeholderData: keepPreviousData,
  });
}
