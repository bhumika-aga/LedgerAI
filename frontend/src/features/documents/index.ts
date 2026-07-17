/**
 * Documents feature boundary (FRONTEND_CODING_STANDARDS §3): upload, listing, and document detail
 * (view, download, delete) within the owning client. Owns its pages, API layer, and server-state
 * hooks; other features consume only this public surface.
 */
export { DocumentDetailPage } from "./DocumentDetailPage";
export { DocumentListPage } from "./DocumentListPage";
