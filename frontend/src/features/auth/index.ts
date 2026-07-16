/**
 * Auth feature boundary (FRONTEND_CODING_STANDARDS §3): sign-in, session handling, and the session
 * bootstrap wiring. Owns its own pages, context, API client, and feature-local types; other features
 * consume only this public surface, never its internals.
 */
export type { User } from "./authApi";
export { AuthProvider, useAuth } from "./AuthContext";
export type { AuthContextValue, AuthStatus } from "./AuthContext";
export { LoginPage } from "./LoginPage";
