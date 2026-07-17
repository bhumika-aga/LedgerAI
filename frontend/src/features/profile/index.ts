/**
 * Profile feature boundary (FRONTEND_CODING_STANDARDS §3): the authenticated user's own profile —
 * viewing and editing their identity fields. Owns its page, API layer, and server-state hooks; other
 * features consume only this public surface.
 *
 * The standards' feature table (§3) predates this capability and does not list it, but it mandates
 * "one module per capability", and User Profile is one of the twelve MVP capabilities (PRD §8.2) with
 * its own backend module — so it gets its own feature rather than being folded into `auth`.
 */
export { ProfilePage } from "./ProfilePage";
export { profileQueryKey, useProfile, useUpdateProfile } from "./useProfile";
