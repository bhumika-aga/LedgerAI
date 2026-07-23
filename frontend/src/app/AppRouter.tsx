import { BrowserRouter, Route, Routes } from "react-router-dom";

import { ActivityTimelinePage } from "../features/activity";
import { LoginPage, RequireAuth } from "../features/auth";
import {
  ClientCreatePage,
  ClientDetailPage,
  ClientEditPage,
  ClientListPage,
} from "../features/clients";
import { DocumentDetailPage, DocumentListPage } from "../features/documents";
import { ProfilePage } from "../features/profile";
import { SearchPage } from "../features/search";
import { AppLayout } from "../shared/layout/AppLayout";
import { HealthPage } from "../system/health/HealthPage";

/**
 * Routing composition point (FRONTEND_CODING_STANDARDS §7).
 *
 * It establishes the router and the shell that wraps routed content: the public sign-in route, the
 * protected authenticated workspace, and the operational System Health page. Protected routes are
 * gated by {@link RequireAuth}, which redirects unauthenticated visitors to sign-in (ARCHITECTURE §6.2,
 * FR-AUTH-006). Client routes mirror the documented API resource shape (API_SPEC §2.2, §7).
 */
export function AppRouter() {
  return (
    <BrowserRouter>
      <AppLayout>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route
            path="/profile"
            element={
              <RequireAuth>
                <ProfilePage />
              </RequireAuth>
            }
          />
          <Route
            path="/clients"
            element={
              <RequireAuth>
                <ClientListPage />
              </RequireAuth>
            }
          />
          <Route
            path="/clients/new"
            element={
              <RequireAuth>
                <ClientCreatePage />
              </RequireAuth>
            }
          />
          <Route
            path="/clients/:clientId"
            element={
              <RequireAuth>
                <ClientDetailPage />
              </RequireAuth>
            }
          />
          <Route
            path="/clients/:clientId/edit"
            element={
              <RequireAuth>
                <ClientEditPage />
              </RequireAuth>
            }
          />
          <Route
            path="/clients/:clientId/documents"
            element={
              <RequireAuth>
                <DocumentListPage />
              </RequireAuth>
            }
          />
          <Route
            path="/documents/:documentId"
            element={
              <RequireAuth>
                <DocumentDetailPage />
              </RequireAuth>
            }
          />
          <Route
            path="/activities"
            element={
              <RequireAuth>
                <ActivityTimelinePage />
              </RequireAuth>
            }
          />
          <Route
            path="/search"
            element={
              <RequireAuth>
                <SearchPage />
              </RequireAuth>
            }
          />
          <Route path="/system/health" element={<HealthPage />} />
        </Routes>
      </AppLayout>
    </BrowserRouter>
  );
}
