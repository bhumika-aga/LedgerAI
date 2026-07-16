import { QueryClient } from "@tanstack/react-query";

/**
 * The React Query client — the single home for server state (ADR-007, FRONTEND_CODING_STANDARDS §6).
 *
 * It is created with library defaults. Query/mutation behavior (retry, staleness, polling for
 * async-ready AI/OCR work) is configured by the feature slices that introduce those calls; nothing
 * business-specific is decided here.
 */
export const queryClient = new QueryClient();
