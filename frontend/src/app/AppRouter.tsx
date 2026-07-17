import { BrowserRouter, Route, Routes } from "react-router-dom";

import { LoginPage } from "../features/auth";
import { ProfilePage } from "../features/profile";
import { AppLayout } from "../shared/layout/AppLayout";
import { HealthPage } from "../system/health/HealthPage";

/**
 * Routing composition point (FRONTEND_CODING_STANDARDS §7).
 *
 * It establishes the router and the shell that wraps routed content: the public sign-in route and the
 * operational System Health page used to validate the stack end to end. Later business feature slices
 * register their own routes (public, protected via a route guard, and nested) here.
 */
export function AppRouter() {
  return (
    <BrowserRouter>
      <AppLayout>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/profile" element={<ProfilePage />} />
          <Route path="/system/health" element={<HealthPage />} />
        </Routes>
      </AppLayout>
    </BrowserRouter>
  );
}
