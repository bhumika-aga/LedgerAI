import { BrowserRouter, Routes } from "react-router-dom";

import { AppLayout } from "../shared/layout/AppLayout";

/**
 * Routing composition point (FRONTEND_CODING_STANDARDS §7).
 *
 * It establishes the router and the shell that wraps routed content. It intentionally defines no
 * routes: feature slices register their own routes (public, protected via a route guard, and nested)
 * inside the {@link Routes} element. No URL structure is invented here.
 */
export function AppRouter() {
  return (
    <BrowserRouter>
      <AppLayout>
        <Routes>
          {/* Feature slices register their <Route> elements here. */}
        </Routes>
      </AppLayout>
    </BrowserRouter>
  );
}
