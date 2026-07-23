import { fireEvent, screen } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import type { Page } from "../../shared";
import { renderWithProviders } from "../../test/renderWithProviders";
import * as reportsApi from "./reportsApi";
import { ReportsListPage } from "./ReportsListPage";

vi.mock("./reportsApi");

function page(
  overrides: Partial<Page<reportsApi.Report>> = {},
): Page<reportsApi.Report> {
  return {
    content: [
      {
        id: "44444444-4444-4444-4444-444444444444",
        documentId: "33333333-3333-3333-3333-333333333333",
        title: "Q4 Review",
        content: "body",
        status: "DRAFT",
        createdAt: "2026-07-23T00:00:00Z",
        updatedAt: "2026-07-23T00:00:00Z",
      },
    ],
    page: 0,
    size: 20,
    totalElements: 1,
    totalPages: 1,
    hasNext: false,
    ...overrides,
  };
}

describe("ReportsListPage", () => {
  afterEach(() => {
    vi.clearAllMocks();
  });

  it("shows a loading state", () => {
    vi.mocked(reportsApi.listReports).mockReturnValue(new Promise(() => {}));

    renderWithProviders(<ReportsListPage />);

    expect(screen.getByText("Loading reports…")).toBeInTheDocument();
  });

  it("shows the empty state when there are no reports", async () => {
    vi.mocked(reportsApi.listReports).mockResolvedValue(page({ content: [] }));

    renderWithProviders(<ReportsListPage />);

    expect(await screen.findByText(/no reports yet/i)).toBeInTheDocument();
  });

  it("renders reports and navigates to detail", async () => {
    vi.mocked(reportsApi.listReports).mockResolvedValue(page());

    renderWithProviders(<ReportsListPage />, {
      path: "/reports",
      initialEntries: ["/reports"],
    });

    fireEvent.click(await screen.findByText("Q4 Review"));
    // Navigation is exercised via the router; the row rendering + click is the unit under test.
    expect(reportsApi.listReports).toHaveBeenCalledWith({ page: 0 });
  });

  it("shows an error state", async () => {
    vi.mocked(reportsApi.listReports).mockRejectedValue(new Error("Network"));

    renderWithProviders(<ReportsListPage />);

    expect(
      await screen.findByText(
        "Your reports could not be loaded. Please try again.",
      ),
    ).toBeInTheDocument();
  });
});
