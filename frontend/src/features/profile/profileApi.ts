import { apiClient, extractValidationErrors } from "../../shared";
import type { User } from "../auth";

// Re-exported for this feature's existing consumers; the implementation is the shared one (§3).
export { extractValidationErrors };

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
