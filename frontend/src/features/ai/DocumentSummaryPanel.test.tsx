import { fireEvent, screen, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import { renderWithProviders } from "../../test/renderWithProviders";
import * as aiApi from "./aiApi";
import { DocumentSummaryPanel } from "./DocumentSummaryPanel";

vi.mock("./aiApi");

const documentId = "33333333-3333-3333-3333-333333333333";

function completed(content: string, edited = false): aiApi.AiResponse {
  return {
    id: "44444444-4444-4444-4444-444444444444",
    type: "SUMMARY",
    status: "COMPLETED",
    documentId,
    prompt: null,
    content,
    edited,
    failureReason: null,
    createdAt: "2026-07-21T00:00:00Z",
    updatedAt: "2026-07-21T00:00:00Z",
  };
}

/** A 404 shaped like an axios error, so the panel treats it as "no summary yet". */
const notFound = { isAxiosError: true, response: { status: 404 } };

function renderPanel() {
  return renderWithProviders(
    <DocumentSummaryPanel documentId={documentId} documentStatus="READY" />,
  );
}

describe("DocumentSummaryPanel", () => {
  beforeEach(() => {
    vi.mocked(aiApi.getSummary).mockRejectedValue(notFound);
  });

  afterEach(() => {
    vi.clearAllMocks();
  });

  it("gates until the document is ready", async () => {
    renderWithProviders(
      <DocumentSummaryPanel
        documentId={documentId}
        documentStatus="PROCESSING"
      />,
    );

    expect(
      await screen.findByText(/can be generated once the document is ready/i),
    ).toBeInTheDocument();
  });

  it("shows the generate state when no summary exists yet", async () => {
    renderPanel();

    expect(
      await screen.findByText(/no summary has been generated/i),
    ).toBeInTheDocument();
    expect(
      screen.getByRole("button", { name: "Generate summary" }),
    ).toBeInTheDocument();
  });

  it("generates a summary and shows it as editable content", async () => {
    vi.mocked(aiApi.generateSummary).mockResolvedValue(
      completed("A grounded summary of the document."),
    );

    renderPanel();
    fireEvent.click(
      await screen.findByRole("button", { name: "Generate summary" }),
    );

    expect(
      await screen.findByDisplayValue("A grounded summary of the document."),
    ).toBeInTheDocument();
    expect(aiApi.generateSummary).toHaveBeenCalledWith(documentId, false);
    // Review-required note (BR-032).
    expect(
      screen.getByText(/review before relying on it/i),
    ).toBeInTheDocument();
  });

  it("shows a failed summary with a retry action", async () => {
    vi.mocked(aiApi.getSummary).mockResolvedValue({
      ...completed(""),
      status: "FAILED",
      content: null,
      failureReason: "The AI service was unavailable.",
    });

    renderPanel();

    expect(
      await screen.findByText("The AI service was unavailable."),
    ).toBeInTheDocument();
    expect(
      screen.getByRole("button", { name: "Try again" }),
    ).toBeInTheDocument();
  });

  it("saves a user edit to the summary", async () => {
    vi.mocked(aiApi.getSummary).mockResolvedValue(
      completed("Original summary."),
    );
    vi.mocked(aiApi.editSummary).mockResolvedValue(
      completed("My edited summary.", true),
    );

    renderPanel();

    const field = await screen.findByLabelText("Summary");
    fireEvent.change(field, { target: { value: "My edited summary." } });
    fireEvent.click(screen.getByRole("button", { name: "Save" }));

    await waitFor(() =>
      expect(aiApi.editSummary).toHaveBeenCalledWith(
        documentId,
        "My edited summary.",
      ),
    );
    expect(await screen.findByText(/\(edited\)/i)).toBeInTheDocument();
  });

  it("shows an error when generation fails", async () => {
    vi.mocked(aiApi.generateSummary).mockRejectedValue(
      new Error("Service Unavailable"),
    );

    renderPanel();
    fireEvent.click(
      await screen.findByRole("button", { name: "Generate summary" }),
    );

    expect(
      await screen.findByText(/could not be generated/i),
    ).toBeInTheDocument();
  });
});
