import { afterEach, describe, expect, it, vi } from "vitest";
import { apiClient } from "../../shared";
import { editSummary, generateSummary, getSummary } from "./aiApi";

const documentId = "33333333-3333-3333-3333-333333333333";

const summary = {
  id: "44444444-4444-4444-4444-444444444444",
  type: "SUMMARY" as const,
  status: "COMPLETED" as const,
  documentId,
  prompt: null,
  content: "A grounded summary.",
  edited: false,
  failureReason: null,
  createdAt: "2026-07-21T00:00:00Z",
  updatedAt: "2026-07-21T00:00:00Z",
};

describe("aiApi", () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("generates a summary via POST without a body by default", async () => {
    const postSpy = vi
      .spyOn(apiClient, "post")
      .mockResolvedValue({ data: summary });

    const result = await generateSummary(documentId);

    expect(postSpy).toHaveBeenCalledWith(
      `/documents/${documentId}/summary`,
      undefined,
    );
    expect(result.content).toBe("A grounded summary.");
  });

  it("sends the regenerate flag when forcing a new summary", async () => {
    const postSpy = vi
      .spyOn(apiClient, "post")
      .mockResolvedValue({ data: summary });

    await generateSummary(documentId, true);

    expect(postSpy).toHaveBeenCalledWith(`/documents/${documentId}/summary`, {
      regenerate: true,
    });
  });

  it("gets the saved summary", async () => {
    const getSpy = vi
      .spyOn(apiClient, "get")
      .mockResolvedValue({ data: summary });

    const result = await getSummary(documentId);

    expect(getSpy).toHaveBeenCalledWith(`/documents/${documentId}/summary`);
    expect(result.status).toBe("COMPLETED");
  });

  it("edits the summary via PATCH with the content", async () => {
    const patchSpy = vi.spyOn(apiClient, "patch").mockResolvedValue({
      data: { ...summary, content: "Edited.", edited: true },
    });

    const result = await editSummary(documentId, "Edited.");

    expect(patchSpy).toHaveBeenCalledWith(`/documents/${documentId}/summary`, {
      content: "Edited.",
    });
    expect(result.edited).toBe(true);
  });
});
