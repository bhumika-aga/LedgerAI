/**
 * AI Chat feature boundary (FRONTEND_CODING_STANDARDS §3): the document-scoped chat surface consuming the
 * AI Chat endpoints (API_SPEC §11). Owns its own component, hooks, and API layer; other features import
 * only this public surface, never its internals.
 */
export { DocumentChatPanel } from "./DocumentChatPanel";
