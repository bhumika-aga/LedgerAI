import { isAxiosError } from "axios";

import { apiClient } from "../../shared";
import type { User } from "../auth";

/**
 * Typed client for the User module (API_SPEC §6). It reuses the shared `apiClient`, which already
 * attaches the Bearer access token and carries credentials, so this layer only describes the two
 * documented calls (FRONTEND_CODING_STANDARDS §8).
 *
 * The subject is always "me": API_SPEC §6.1 confirms there is no `/users/{id}` in the MVP, so no id is
 * ever sent — the backend resolves the user from the token.
 */

/**
 * Free-form preferences (DATABASE §5.1: jsonb, "Basic UI/app preferences"). No document defines the
 * keys, so the type stays opaque rather than inventing a shape.
 */
export type Preferences = Record<string, unknown>;

/** Editable profile fields (API_SPEC §6.2). An omitted field is left unchanged (PATCH, §2.3). */
export interface UpdateProfileInput {
  fullName?: string;
  professionalDetails?: string;
  preferences?: Preferences;
}

export async function getProfile(): Promise<User> {
  const { data } = await apiClient.get<User>("/users/me");
  return data;
}

export async function updateProfile(input: UpdateProfileInput): Promise<User> {
  const { data } = await apiClient.patch<User>("/users/me", input);
  return data;
}

/**
 * Reads the field-level messages out of a `422` Problem Details response (API_SPEC §2.12, §6.2) so the
 * form can show them against the right inputs. The server is the source of truth for VR-003 — its
 * limits are configured, so the client cannot mirror them without drifting, and client-side checks are
 * a UX aid only (SECURITY §Trust Boundaries). Anything else yields no field errors and is surfaced as
 * a general failure.
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
