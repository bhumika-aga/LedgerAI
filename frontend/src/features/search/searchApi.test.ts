import { afterEach, describe, expect, it, vi } from "vitest";
import { apiClient } from "../../shared";
import { searchDocuments } from "./searchApi";

const result = {
  documentId: "33333333-3333-3333-3333-333333333333",
  clientId: "22222222-2222-2222-2222-222222222222",
  title: "statement.pdf",
  snippet: "Balance sheet total 987654",
  matchContext: "…balance sheet total 987654…",
  updatedAt: "2026-07-21T00:00:00Z",
};

const page = {
  content: [result],
  page: 0,
  size: 20,
  totalElements: 1,
  totalPages: 1,
  hasNext: false,
};

describe("searchApi", () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("searches, passing the query as params", async () => {
    const getSpy = vi.spyOn(apiClient, "get").mockResolvedValue({ data: page });

    const data = await searchDocuments({ q: "balance", page: 0 });

    expect(getSpy).toHaveBeenCalledWith("/search", {
      params: { q: "balance", page: 0 },
    });
    expect(data.content[0].documentId).toBe(result.documentId);
  });
});
