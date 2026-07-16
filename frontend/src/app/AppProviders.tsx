import { CssBaseline, ThemeProvider } from "@mui/material";
import { QueryClientProvider } from "@tanstack/react-query";
import type { ReactNode } from "react";

import { AuthProvider } from "../features/auth";
import { ErrorBoundary } from "../shared/error/ErrorBoundary";
import { theme } from "../shared/theme/theme";
import { queryClient } from "./queryClient";

/**
 * Cross-cutting application providers, composed once around the whole app.
 *
 * This is the single place that establishes the server-state client (React Query), the design-system
 * theme (Material UI), the session context (auth), and the global error boundary. Each is a
 * composition point the feature slices build on; none carries business behavior of its own.
 */
export function AppProviders({ children }: { children: ReactNode }) {
  return (
    <ErrorBoundary>
      <QueryClientProvider client={queryClient}>
        <ThemeProvider theme={theme}>
          <CssBaseline />
          <AuthProvider>{children}</AuthProvider>
        </ThemeProvider>
      </QueryClientProvider>
    </ErrorBoundary>
  );
}
