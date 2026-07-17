/**
 * Clients feature boundary (FRONTEND_CODING_STANDARDS §3): client management views and interactions —
 * list, workspace/detail, create, and edit. Owns its pages, form, API layer, and server-state hooks;
 * other features consume only this public surface.
 */
export { ClientCreatePage } from "./ClientCreatePage";
export { ClientDetailPage } from "./ClientDetailPage";
export { ClientEditPage } from "./ClientEditPage";
export { ClientListPage } from "./ClientListPage";
