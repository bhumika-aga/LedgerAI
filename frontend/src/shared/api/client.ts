import type { AxiosInstance } from "axios";
import axios from "axios";

/**
 * The centralized API client (FRONTEND_CODING_STANDARDS §8, ADR-007).
 *
 * This single Axios instance is the only gateway to the backend; all future feature code calls typed
 * functions built on it rather than issuing raw HTTP. It is configured with the API base URL only.
 * The cross-cutting behavior the standards require here — attaching auth tokens, refreshing them, and
 * normalizing RFC 7807 errors — is added as interceptors by the auth and error-handling slices; this
 * scaffold defines no endpoints, request/response types, or interceptors.
 *
 * The base URL is environment-driven (no host is hard-coded); it falls back to the same-origin
 * {@code /api/v1} root (ADR-011) for local development behind a proxy.
 */
export const apiClient: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? "/api/v1",
});
