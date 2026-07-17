import { afterEach, describe, expect, it, vi } from "vitest";
import { apiClient } from "../../shared";
import {
  deleteDocument,
  getDocument,
  getDownload,
  listDocuments,
  uploadDocument,
} from "./documentsApi";

const document = {
  id: "33333333-3333-3333-3333-333333333333",
  clientId: "22222222-2222-2222-2222-222222222222",
  originalFilename: "statement.pdf",
  mimeType: "application/pdf",
  sizeBytes: 1234,
  status: "UPLOADED" as const,
  extractionMethod: null,
  failureReason: null,
  createdAt: "2026-07-17T00:00:00Z",
  updatedAt: "2026-07-17T00:00:00Z",
};

const page = {
  content: [document],
  page: 0,
  size: 20,
  totalElements: 1,
  totalPages: 1,
  hasNext: false,
};

describe("documentsApi", () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("lists a client's documents, passing the query as params", async () => {
    const getSpy = vi.spyOn(apiClient, "get").mockResolvedValue({ data: page });

    const result = await listDocuments(document.clientId, {
      status: "UPLOADED",
      page: 0,
      size: 20,
    });

    expect(getSpy).toHaveBeenCalledWith(
      `/clients/${document.clientId}/documents`,
      {
        params: { status: "UPLOADED", page: 0, size: 20 },
      },
    );
    expect(result.content[0].originalFilename).toBe("statement.pdf");
  });

  it("gets a single document", async () => {
    const getSpy = vi
      .spyOn(apiClient, "get")
      .mockResolvedValue({ data: document });

    await getDocument(document.id);

    expect(getSpy).toHaveBeenCalledWith(`/documents/${document.id}`);
  });

  it("uploads a file as multipart form data", async () => {
    const postSpy = vi
      .spyOn(apiClient, "post")
      .mockResolvedValue({ data: document });
    const file = new File(
      [new Uint8Array([0x25, 0x50, 0x44, 0x46])],
      "statement.pdf",
      {
        type: "application/pdf",
      },
    );

    await uploadDocument(document.clientId, file);

    expect(postSpy).toHaveBeenCalledTimes(1);
    const [url, body] = postSpy.mock.calls[0];
    expect(url).toBe(`/clients/${document.clientId}/documents`);
    expect(body).toBeInstanceOf(FormData);
    expect((body as FormData).get("file")).toBe(file);
  });

  it("gets a download reference", async () => {
    const download = {
      downloadUrl: "https://storage.test/download/ref",
      expiresAt: "2026-07-17T00:05:00Z",
      mimeType: "application/pdf",
      originalFilename: "statement.pdf",
      sizeBytes: 1234,
    };
    const getSpy = vi
      .spyOn(apiClient, "get")
      .mockResolvedValue({ data: download });

    const result = await getDownload(document.id);

    expect(getSpy).toHaveBeenCalledWith(`/documents/${document.id}/download`);
    expect(result.downloadUrl).toBe("https://storage.test/download/ref");
  });

  it("deletes a document via DELETE", async () => {
    const deleteSpy = vi
      .spyOn(apiClient, "delete")
      .mockResolvedValue({ data: undefined });

    await deleteDocument(document.id);

    expect(deleteSpy).toHaveBeenCalledWith(`/documents/${document.id}`);
  });
});
