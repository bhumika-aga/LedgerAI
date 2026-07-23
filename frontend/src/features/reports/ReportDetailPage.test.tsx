import { fireEvent, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { renderWithProviders } from "../../test/renderWithProviders";
import { ReportDetailPage } from "./ReportDetailPage";
import * as reportsApi from "./reportsApi";

vi.mock("./reportsApi");

const reportId = "44444444-4444-4444-4444-444444444444";

const report: reportsApi.Report = {
  id: reportId,
  documentId: "33333333-3333-3333-3333-333333333333",
  title: "Q4 Review",
  content: "The generated report body.",
  status: "DRAFT",
  createdAt: "2026-07-23T00:00:00Z",
  updatedAt: "2026-07-23T00:00:00Z",
};

function renderDetail() {
  return renderWithProviders(<ReportDetailPage />, {
    path: "/reports/:reportId",
    initialEntries: [`/reports/${reportId}`],
  });
}

describe("ReportDetailPage", () => {
  afterEach(() => {
    vi.clearAllMocks();
  });

  it("shows the not-found state when the report cannot be loaded", async () => {
    vi.mocked(reportsApi.getReport).mockRejectedValue(new Error("Not Found"));

    renderDetail();

    expect(
      await screen.findByText("This report could not be found."),
    ).toBeInTheDocument();
  });

  it("shows the report and saves an edit", async () => {
    vi.mocked(reportsApi.getReport).mockResolvedValue(report);
    vi.mocked(reportsApi.updateReport).mockResolvedValue({
      ...report,
      content: "My edited report.",
    });

    renderDetail();

    const field = await screen.findByLabelText("Report");
    fireEvent.change(field, { target: { value: "My edited report." } });
    fireEvent.click(screen.getByRole("button", { name: "Save" }));

    await waitFor(() =>
      expect(reportsApi.updateReport).toHaveBeenCalledWith(reportId, {
        title: "Q4 Review",
        content: "My edited report.",
      }),
    );
    // Review-required note (BR-032, FR-RPT-005).
    expect(
      screen.getByText(/review before relying on it/i),
    ).toBeInTheDocument();
  });

  it("marks a draft as saved", async () => {
    vi.mocked(reportsApi.getReport).mockResolvedValue(report);
    vi.mocked(reportsApi.updateReport).mockResolvedValue({
      ...report,
      status: "SAVED",
    });

    renderDetail();

    fireEvent.click(
      await screen.findByRole("button", { name: "Mark as saved" }),
    );

    await waitFor(() =>
      expect(reportsApi.updateReport).toHaveBeenCalledWith(reportId, {
        status: "SAVED",
      }),
    );
  });

  it("deletes the report", async () => {
    vi.mocked(reportsApi.getReport).mockResolvedValue(report);
    vi.mocked(reportsApi.deleteReport).mockResolvedValue();

    renderDetail();

    fireEvent.click(await screen.findByRole("button", { name: "Delete" }));

    await waitFor(() =>
      expect(reportsApi.deleteReport).toHaveBeenCalledWith(reportId),
    );
  });
});
