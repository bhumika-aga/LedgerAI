import type { Page } from "../../shared";
import { apiClient } from "../../shared";

/**
 * Typed client for the Search module (API_SPEC §14). Reuses the shared `apiClient` (Bearer + cookies) and
 * shared `Page`, describing only the one documented operation (FRONTEND_CODING_STANDARDS §8) — global
 * keyword search over the caller's document content. There are no filters, suggestions, or autocomplete
 * calls beyond the documented `q`/`page`/`size` parameters.
 */

/** SearchResultResponse (API_SPEC §17.7). */
export interface SearchResult {
  documentId: string;
  clientId: string;
  title: string;
  snippet: string;
  matchContext: string;
  updatedAt: string;
}

export interface SearchQuery {
  q: string;
  page?: number;
  size?: number;
}

/** API_SPEC §14.1: keyword search over the caller's documents, paged. */
export async function searchDocuments(
  query: SearchQuery,
): Promise<Page<SearchResult>> {
  const { data } = await apiClient.get<Page<SearchResult>>("/search", {
    params: query,
  });
  return data;
}
