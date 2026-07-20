/**
 * AI feature boundary (FRONTEND_CODING_STANDARDS §3): AI interaction surfaces consuming the AI
 * endpoints. Owns its own components, hooks, and API layer; other features import only this public
 * surface, never its internals.
 *
 * <p>This slice implements <strong>AI Summary only</strong> (API_SPEC §10). Chat, email, and report
 * surfaces are documented but not built here.
 */
export { DocumentSummaryPanel } from "./DocumentSummaryPanel";
