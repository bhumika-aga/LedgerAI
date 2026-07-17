import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render } from "@testing-library/react";
import type { ReactElement, ReactNode } from "react";
import { MemoryRouter, Route, Routes } from "react-router-dom";

/**
 * Shared test infrastructure (TESTING_STRATEGY §8): render a component inside a fresh React Query
 * client and an in-memory router, so page tests exercise the same providers the app composes without
 * each test reinventing the wrapper.
 *
 * <p>Retries are disabled so a rejected query fails fast rather than stalling the test. When `path`/
 * `initialEntries` are given, the element is mounted as a route so `useParams`/`useNavigate` work.
 */
export function renderWithProviders(
  ui: ReactElement,
  options: { path?: string; initialEntries?: string[] } = {},
) {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });

  const wrapped: ReactNode =
    options.path !== undefined ? (
      <Routes>
        <Route path={options.path} element={ui} />
      </Routes>
    ) : (
      ui
    );

  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={options.initialEntries ?? ["/"]}>
        {wrapped}
      </MemoryRouter>
    </QueryClientProvider>,
  );
}
