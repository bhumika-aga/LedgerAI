/**
 * Reports feature boundary (FRONTEND_CODING_STANDARDS §3): report generation, listing, viewing, editing,
 * export (client-side), and deletion. Owns its own pages, components, API layer, and hooks; other features
 * consume only this public surface, never its internals.
 */
export { DocumentReportsPanel } from "./DocumentReportsPanel";
export { ReportDetailPage } from "./ReportDetailPage";
export { ReportsListPage } from "./ReportsListPage";
