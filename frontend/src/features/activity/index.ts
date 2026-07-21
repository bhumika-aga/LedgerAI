/**
 * Activity feature boundary (FRONTEND_CODING_STANDARDS §3): the activity timeline view. Owns its own
 * page, API layer, and server-state hook; other features consume only this public surface, never its
 * internals. The timeline is read-only (FR-TMLN-004, BR-016).
 */
export { ActivityTimelinePage } from "./ActivityTimelinePage";
