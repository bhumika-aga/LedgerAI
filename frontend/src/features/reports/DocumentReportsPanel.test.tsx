import { fireEvent, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import type { Page } from "../../shared";
import { renderWithProviders } from "../../test/renderWithProviders";
import { DocumentReportsPanel } from "./DocumentReportsPanel";
import * as reportsApi from "./reportsApi";

vi.mock("./reportsApi");

const documentId = "33333333-3333-3333-3333-333333333333";

function emptyPage(): Page<reportsApi.Report> {
  return {
    content: [],
    page: 0,
    size: 20,
    totalElements: 0,
    totalPages: 0,
    hasNext: false,
  };
}

const draft: reportsApi.Report = {
  id: "44444444-4444-4444-4444-444444444444",
  documentId,
  title: "Q4",
  content: "body",
  status: "DRAFT",
  createdAt: "2026-07-23T00:00:00Z",
  updatedAt: "2026-07-23T00:00:00Z",
};

function renderPanel(status: "READY" | "PROCESSING" = "READY") {
  return renderWithProviders(
    <DocumentReportsPanel documentId={documentId} documentStatus={status} />,
    {
      path: "/documents/:documentId",
      initialEntries: [`/documents/${documentId}`],
    },
  );
}

describe("DocumentReportsPanel", () => {
  afterEach(() => {
    vi.clearAllMocks();
  });

  it("gates until the document is ready", () => {
    renderPanel("PROCESSING");

    expect(
      screen.getByText(/can be generated once the document is ready/i),
    ).toBeInTheDocument();
    expect(reportsApi.listReports).not.toHaveBeenCalled();
  });

  it("shows the empty state and a generate button when ready", async () => {
    vi.mocked(reportsApi.listReports).mockResolvedValue(emptyPage());

    renderPanel();

    expect(
      await screen.findByText(/no reports for this document yet/i),
    ).toBeInTheDocument();
    expect(
      screen.getByRole("button", { name: "Generate report" }),
    ).toBeInTheDocument();
  });

  it("lists existing reports for the document", async () => {
    vi.mocked(reportsApi.listReports).mockResolvedValue({
      ...emptyPage(),
      content: [draft],
      totalElements: 1,
    });

    renderPanel();

    expect(await screen.findByText("Q4")).toBeInTheDocument();
    expect(reportsApi.listReports).toHaveBeenCalledWith({ documentId });
  });

  it("generates a report", async () => {
    vi.mocked(reportsApi.listReports).mockResolvedValue(emptyPage());
    vi.mocked(reportsApi.generateReport).mockResolvedValue(draft);

    renderPanel();

    fireEvent.click(
      await screen.findByRole("button", { name: "Generate report" }),
    );

    await waitFor(() =>
      expect(reportsApi.generateReport).toHaveBeenCalledWith(
        documentId,
        undefined,
      ),
    );
  });

  it("shows an error when generation fails", async () => {
    vi.mocked(reportsApi.listReports).mockResolvedValue(emptyPage());
    vi.mocked(reportsApi.generateReport).mockRejectedValue(new Error("503"));

    renderPanel();

    fireEvent.click(
      await screen.findByRole("button", { name: "Generate report" }),
    );

    expect(
      await screen.findByText(/could not be generated/i),
    ).toBeInTheDocument();
  });
});
