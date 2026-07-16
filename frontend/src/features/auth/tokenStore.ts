/**
 * In-memory access-token store (ADR-018, SECURITY §7).
 *
 * The short-lived access token is held only in memory — never in localStorage or a cookie — so it is
 * not readable across page loads and not exposed to XSS-persisted theft. It is re-established on each
 * load from the httpOnly refresh cookie (session bootstrap). The refresh token itself never passes
 * through here; the browser holds it as an httpOnly cookie the JS cannot read.
 */
let accessToken: string | null = null;

export function getAccessToken(): string | null {
  return accessToken;
}

export function setAccessToken(token: string): void {
  accessToken = token;
}

export function clearAccessToken(): void {
  accessToken = null;
}
