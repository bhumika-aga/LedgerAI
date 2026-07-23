import { afterEach, describe, expect, it, vi } from "vitest";
import { apiClient } from "../../shared";
import { askQuestion, getChatHistory } from "./chatApi";

const documentId = "33333333-3333-3333-3333-333333333333";

const exchange = {
  id: "44444444-4444-4444-4444-444444444444",
  type: "CHAT" as const,
  status: "COMPLETED" as const,
  documentId,
  prompt: "What is the total?",
  content: "The total is 987654.",
  edited: false,
  failureReason: null,
  createdAt: "2026-07-23T00:00:00Z",
  updatedAt: "2026-07-23T00:00:00Z",
};

const page = {
  content: [exchange],
  page: 0,
  size: 20,
  totalElements: 1,
  totalPages: 1,
  hasNext: false,
};

describe("chatApi", () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("asks a question via POST with the question body", async () => {
    const postSpy = vi
      .spyOn(apiClient, "post")
      .mockResolvedValue({ data: exchange });

    await askQuestion(documentId, "What is the total?");

    expect(postSpy).toHaveBeenCalledWith(`/documents/${documentId}/chat`, {
      question: "What is the total?",
    });
  });

  it("fetches chat history via GET, passing the query as params", async () => {
    const getSpy = vi.spyOn(apiClient, "get").mockResolvedValue({ data: page });

    await getChatHistory(documentId, { page: 1 });

    expect(getSpy).toHaveBeenCalledWith(`/documents/${documentId}/chat`, {
      params: { page: 1 },
    });
  });
});
