import { isAxiosError } from "axios";

/**
 * Shared helpers for the RFC 7807 Problem Details error model (API_SPEC §2.12).
 *
 * This is genuinely cross-cutting: every feature that submits a form reads `422` field errors the same
 * way, so the parsing lives in `shared` and is reused rather than reimplemented per feature
 * (FRONTEND_CODING_STANDARDS §3).
 */

/**
 * Reads the field-level messages out of a `422` Problem Details response so a form can show them
 * against the right inputs. The server is the source of truth for validation — its limits are
 * configured, so the client cannot mirror them without drifting, and client-side checks are a UX aid
 * only (SECURITY §Trust Boundaries). Anything that is not a well-formed `422` yields no field errors
 * and should be surfaced as a general failure.
 */
export function extractValidationErrors(
  error: unknown,
): Record<string, string> {
  if (!isAxiosError(error) || error.response?.status !== 422) {
    return {};
  }
  const validationErrors: unknown = error.response.data?.validationErrors;
  if (!Array.isArray(validationErrors)) {
    return {};
  }
  const entries = validationErrors
    .filter(
      (item): item is { field: string; message: string } =>
        typeof item?.field === "string" && typeof item?.message === "string",
    )
    .map((item) => [item.field, item.message] as const);
  return Object.fromEntries(entries);
}
