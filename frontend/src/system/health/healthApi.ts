import { apiClient } from "../../shared";

/**
 * The single API call for the System Health slice.
 *
 * It targets the operational Actuator health endpoint (ADR-015), which lives at the backend server
 * root — outside the {@code /api/v1} business API base — so the call overrides the client's base URL
 * to the backend origin. No business endpoint, DTO, or authentication is involved; the call resolves
 * when the backend is reachable and reports healthy, and rejects otherwise.
 */
const apiBaseUrl = import.meta.env.VITE_API_BASE_URL ?? "/api/v1";
const healthBaseUrl = apiBaseUrl.replace(/\/api\/v1\/?$/, "");

export async function checkBackendHealth(): Promise<boolean> {
  const response = await apiClient.get("/actuator/health", {
    baseURL: healthBaseUrl,
  });
  return response.status === 200;
}
