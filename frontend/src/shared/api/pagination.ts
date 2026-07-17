/**
 * The pagination envelope every paged endpoint returns (API_SPEC §17.9, §2.5):
 * {@code { content, page, size, totalElements, totalPages, hasNext }}.
 *
 * Cross-cutting and feature-agnostic, so it lives in `shared` and is reused by every feature that
 * lists a resource rather than being redeclared per feature (FRONTEND_CODING_STANDARDS §3).
 */
export interface Page<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  hasNext: boolean;
}
