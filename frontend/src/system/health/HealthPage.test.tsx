import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen } from "@testing-library/react";
import type { ReactElement } from "react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { checkBackendHealth } from "./healthApi";
import { HealthPage } from "./HealthPage";

vi.mock("./healthApi");

function renderWithClient(ui: ReactElement) {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  return render(
    <QueryClientProvider client={queryClient}>{ui}</QueryClientProvider>,
  );
}

describe("HealthPage", () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("shows the backend as reachable when the health check succeeds", async () => {
    vi.mocked(checkBackendHealth).mockResolvedValue(true);

    renderWithClient(<HealthPage />);

    expect(await screen.findByText("Backend reachable")).toBeInTheDocument();
  });

  it("shows the backend as unreachable when the health check fails", async () => {
    vi.mocked(checkBackendHealth).mockRejectedValue(new Error("Network Error"));

    renderWithClient(<HealthPage />);

    expect(await screen.findByText("Backend unreachable")).toBeInTheDocument();
  });
});
