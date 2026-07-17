import { fireEvent, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { renderWithProviders } from "../../test/renderWithProviders";
import { DocumentDetailPage } from "./DocumentDetailPage";
import * as documentsApi from "./documentsApi";

vi.mock("./documentsApi");

const document: documentsApi.Document = {
  id: "33333333-3333-3333-3333-333333333333",
  clientId: "22222222-2222-2222-2222-222222222222",
  originalFilename: "statement.pdf",
  mimeType: "application/pdf",
  sizeBytes: 1234,
  status: "UPLOADED",
  extractionMethod: null,
  failureReason: null,
  createdAt: "2026-07-17T00:00:00Z",
  updatedAt: "2026-07-17T00:00:00Z",
};

function renderDetailPage() {
  return renderWithProviders(<DocumentDetailPage />, {
    path: "/documents/:documentId",
    initialEntries: [`/documents/${document.id}`],
  });
}

describe("DocumentDetailPage", () => {
  afterEach(() => {
    vi.clearAllMocks();
  });

  it("shows the document once loaded", async () => {
    vi.mocked(documentsApi.getDocument).mockResolvedValue(document);

    renderDetailPage();

    expect(
      await screen.findByRole("heading", { name: "statement.pdf" }),
    ).toBeInTheDocument();
    expect(screen.getByText("application/pdf")).toBeInTheDocument();
    expect(documentsApi.getDocument).toHaveBeenCalledWith(document.id);
  });

  it("shows the not-found state when the document cannot be loaded", async () => {
    // A non-owned document returns 404 from the server (BR-004); the UI does not distinguish it from a
    // genuinely missing one (SECURITY §5).
    vi.mocked(documentsApi.getDocument).mockRejectedValue(
      new Error("Not Found"),
    );

    renderDetailPage();

    expect(
      await screen.findByText("This document could not be found."),
    ).toBeInTheDocument();
  });

  it("fetches a download link and opens it", async () => {
    vi.mocked(documentsApi.getDocument).mockResolvedValue(document);
    vi.mocked(documentsApi.getDownload).mockResolvedValue({
      downloadUrl: "https://storage.test/download/ref",
      expiresAt: "2026-07-17T00:05:00Z",
      mimeType: "application/pdf",
      originalFilename: "statement.pdf",
      sizeBytes: 1234,
    });
    const openSpy = vi.spyOn(window, "open").mockReturnValue(null);

    renderDetailPage();
    fireEvent.click(await screen.findByRole("button", { name: "Download" }));

    await waitFor(() =>
      expect(documentsApi.getDownload).toHaveBeenCalledWith(document.id),
    );
    await waitFor(() =>
      expect(openSpy).toHaveBeenCalledWith(
        "https://storage.test/download/ref",
        "_blank",
        "noopener",
      ),
    );
  });

  it("deletes the document", async () => {
    vi.mocked(documentsApi.getDocument).mockResolvedValue(document);
    vi.mocked(documentsApi.deleteDocument).mockResolvedValue();

    renderDetailPage();
    fireEvent.click(await screen.findByRole("button", { name: "Delete" }));

    await waitFor(() => expect(documentsApi.deleteDocument).toHaveBeenCalled());
    expect(vi.mocked(documentsApi.deleteDocument).mock.calls[0][0]).toBe(
      document.id,
    );
  });

  it("shows an error when deletion fails", async () => {
    vi.mocked(documentsApi.getDocument).mockResolvedValue(document);
    vi.mocked(documentsApi.deleteDocument).mockRejectedValue(
      new Error("Network Error"),
    );

    renderDetailPage();
    fireEvent.click(await screen.findByRole("button", { name: "Delete" }));

    expect(
      await screen.findByText(
        "This document could not be deleted. Please try again.",
      ),
    ).toBeInTheDocument();
  });
});
