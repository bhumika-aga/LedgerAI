/**
 * Shared module boundary (FRONTEND_CODING_STANDARDS §3).
 *
 * Holds genuinely cross-cutting, feature-agnostic infrastructure: the centralized API client, the
 * design-system theme, the application shell, and the global error boundary. Anything that "knows"
 * about a feature (clients, documents, …) belongs in that feature, never here. This barrel is the
 * module's public surface; feature slices import shared code from here, not from internal paths.
 */
export { apiClient } from "./api/client";
export { extractValidationErrors } from "./api/problemDetails";
export { ErrorBoundary } from "./error/ErrorBoundary";
export { AppLayout } from "./layout/AppLayout";
export { theme } from "./theme/theme";
