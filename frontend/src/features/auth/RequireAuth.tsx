import type { ReactNode } from "react";
import { Navigate, useLocation } from "react-router-dom";

import { useAuth } from "./AuthContext";

/**
 * Route guard (FRONTEND_CODING_STANDARDS §4 — Route Guard; ARCHITECTURE §6.2).
 *
 * Gates protected routes on authentication state: while the session is being restored it renders
 * nothing (avoiding a flash of the sign-in page for an already-authenticated user), and an
 * unauthenticated visitor is redirected to sign-in (FR-AUTH-006). The attempted location is passed
 * along so a future sign-in flow can return the user there.
 *
 * <p>This is <strong>UX only</strong>, never a security control — the server enforces authorization on
 * every request (SECURITY §5). Hiding a route is not the protection; the API's ownership checks are.
 */
export function RequireAuth({ children }: { children: ReactNode }) {
  const { status } = useAuth();
  const location = useLocation();

  if (status === "loading") {
    return <p role="status">Loading…</p>;
  }

  if (status === "unauthenticated") {
    return <Navigate to="/login" replace state={{ from: location }} />;
  }

  return <>{children}</>;
}
