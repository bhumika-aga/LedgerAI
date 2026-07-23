import { afterEach, describe, expect, it, vi } from "vitest";
import { apiClient } from "../../shared";
import { generateEmail } from "./emailApi";

const draft = {
  id: "44444444-4444-4444-4444-444444444444",
  type: "EMAIL" as const,
  status: "COMPLETED" as const,
  documentId: null,
  prompt: "Write a follow-up.",
  content: "Dear client, ...",
  edited: false,
  failureReason: null,
  createdAt: "2026-07-24T00:00:00Z",
  updatedAt: "2026-07-24T00:00:00Z",
};

describe("emailApi", () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("generates a draft via POST with the instruction only", async () => {
    const postSpy = vi
      .spyOn(apiClient, "post")
      .mockResolvedValue({ data: draft });

    await generateEmail({ instruction: "Write a follow-up." });

    expect(postSpy).toHaveBeenCalledWith("/ai/emails", {
      instruction: "Write a follow-up.",
    });
  });

  it("passes optional client and document context", async () => {
    const postSpy = vi
      .spyOn(apiClient, "post")
      .mockResolvedValue({ data: draft });

    await generateEmail({
      instruction: "Chase the invoice.",
      clientId: "c-1",
      documentId: "d-1",
    });

    expect(postSpy).toHaveBeenCalledWith("/ai/emails", {
      instruction: "Chase the invoice.",
      clientId: "c-1",
      documentId: "d-1",
    });
  });
});
