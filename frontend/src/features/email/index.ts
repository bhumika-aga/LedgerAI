/**
 * AI Email feature boundary (FRONTEND_CODING_STANDARDS §3): the email generation surface consuming the AI
 * Email endpoint (API_SPEC §12). Owns its own page, hook, and API layer; other features import only this
 * public surface, never its internals.
 */
export { EmailGenerationPage } from "./EmailGenerationPage";
