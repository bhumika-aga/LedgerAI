/**
 * Search feature boundary (FRONTEND_CODING_STANDARDS §3): the global search view. Owns its own page, API
 * layer, and server-state hook; other features consume only this public surface, never its internals.
 * Search is read-only, over already-extracted document content.
 */
export { SearchPage } from "./SearchPage";
