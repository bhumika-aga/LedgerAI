import { fireEvent, screen, waitFor } from "@testing-library/react";
import { AxiosError, AxiosHeaders } from "axios";
import { afterEach, describe, expect, it, vi } from "vitest";

import { renderWithProviders } from "../../test/renderWithProviders";
import { DocumentListPage } from "./DocumentListPage";
import * as documentsApi from "./documentsApi";

vi.mock("./documentsApi");

const clientId = "22222222-2222-2222-2222-222222222222";

const document: documentsApi.Document = {
  id: "33333333-3333-3333-3333-333333333333",
  clientId,
  originalFilename: "statement.pdf",
  mimeType: "application/pdf",
  sizeBytes: 1234,
  status: "UPLOADED",
  extractionMethod: null,
  failureReason: null,
  createdAt: "2026-07-17T00:00:00Z",
  updatedAt: "2026-07-17T00:00:00Z",
};

function pageOf(
  docs: documentsApi.Document[],
): import("../../shared").Page<documentsApi.Document> {
  return {
    content: docs,
    page: 0,
    size: 20,
    totalElements: docs.length,
    totalPages: 1,
    hasNext: false,
  };
}

function validationError(field: string, message: string): AxiosError {
  const error = new AxiosError("Unprocessable Entity");
  error.response = {
    status: 422,
    data: {
      type: "/problems/validation-error",
      validationErrors: [{ field, message }],
    },
    statusText: "",
    headers: new AxiosHeaders(),
    config: { headers: new AxiosHeaders() },
  };
  return error;
}

function renderListPage() {
  return renderWithProviders(<DocumentListPage />, {
    path: "/clients/:clientId/documents",
    initialEntries: [`/clients/${clientId}/documents`],
  });
}

describe("DocumentListPage", () => {
  afterEach(() => {
    vi.clearAllMocks();
  });

  it("shows a loading state while documents are fetched", () => {
    vi.mocked(documentsApi.listDocuments).mockReturnValue(
      new Promise(() => {}),
    );

    renderListPage();

    expect(screen.getByRole("status")).toHaveTextContent("Loading documents…");
  });

  it("shows the empty state when there are no documents", async () => {
    vi.mocked(documentsApi.listDocuments).mockResolvedValue(pageOf([]));

    renderListPage();

    expect(await screen.findByText(/no documents yet/i)).toBeInTheDocument();
  });

  it("lists documents when there are some", async () => {
    vi.mocked(documentsApi.listDocuments).mockResolvedValue(pageOf([document]));

    renderListPage();

    expect(await screen.findByText("statement.pdf")).toBeInTheDocument();
  });

  it("shows an error state when the list cannot be loaded", async () => {
    vi.mocked(documentsApi.listDocuments).mockRejectedValue(
      new Error("Network Error"),
    );

    renderListPage();

    expect(
      await screen.findByText(
        "Your documents could not be loaded. Please try again.",
      ),
    ).toBeInTheDocument();
  });

  it("uploads a chosen file and confirms success", async () => {
    vi.mocked(documentsApi.listDocuments).mockResolvedValue(pageOf([]));
    vi.mocked(documentsApi.uploadDocument).mockResolvedValue(document);

    renderListPage();
    await screen.findByText(/no documents yet/i);

    const file = new File([new Uint8Array([0x25, 0x50])], "statement.pdf", {
      type: "application/pdf",
    });
    fireEvent.change(screen.getByLabelText("Upload document"), {
      target: { files: [file] },
    });

    expect(await screen.findByText("Document uploaded.")).toBeInTheDocument();
    await waitFor(() =>
      expect(documentsApi.uploadDocument).toHaveBeenCalledWith(clientId, file),
    );
  });

  it("shows the server's validation message when an upload is rejected", async () => {
    vi.mocked(documentsApi.listDocuments).mockResolvedValue(pageOf([]));
    vi.mocked(documentsApi.uploadDocument).mockRejectedValue(
      validationError("file", "Unsupported file type."),
    );

    renderListPage();
    await screen.findByText(/no documents yet/i);

    const file = new File(["text"], "notes.txt", { type: "text/plain" });
    fireEvent.change(screen.getByLabelText("Upload document"), {
      target: { files: [file] },
    });

    expect(
      await screen.findByText("Unsupported file type."),
    ).toBeInTheDocument();
  });

  it("shows a general error when an upload fails without field errors", async () => {
    vi.mocked(documentsApi.listDocuments).mockResolvedValue(pageOf([]));
    vi.mocked(documentsApi.uploadDocument).mockRejectedValue(
      new Error("Network Error"),
    );

    renderListPage();
    await screen.findByText(/no documents yet/i);

    const file = new File([new Uint8Array([0x25, 0x50])], "statement.pdf", {
      type: "application/pdf",
    });
    fireEvent.change(screen.getByLabelText("Upload document"), {
      target: { files: [file] },
    });

    expect(
      await screen.findByText(
        "Your document could not be uploaded. Please try again.",
      ),
    ).toBeInTheDocument();
  });
});
