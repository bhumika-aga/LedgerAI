import { fireEvent, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import type { Page } from "../../shared";
import { renderWithProviders } from "../../test/renderWithProviders";
import * as searchApi from "./searchApi";
import { SearchPage } from "./SearchPage";

vi.mock("./searchApi");

function page(
  overrides: Partial<Page<searchApi.SearchResult>> = {},
): Page<searchApi.SearchResult> {
  return {
    content: [
      {
        documentId: "33333333-3333-3333-3333-333333333333",
        clientId: "22222222-2222-2222-2222-222222222222",
        title: "statement.pdf",
        snippet: "Balance sheet total 987654",
        matchContext: "…balance sheet total 987654…",
        updatedAt: "2026-07-21T00:00:00Z",
      },
    ],
    page: 0,
    size: 20,
    totalElements: 1,
    totalPages: 1,
    hasNext: false,
    ...overrides,
  };
}

function submitSearch(term: string) {
  fireEvent.change(screen.getByLabelText("Search documents"), {
    target: { value: term },
  });
  fireEvent.click(screen.getByRole("button", { name: "Search" }));
}

describe("SearchPage", () => {
  afterEach(() => {
    vi.clearAllMocks();
  });

  it("prompts for a keyword before any search runs", () => {
    renderWithProviders(<SearchPage />);

    expect(
      screen.getByText("Enter a keyword to search your documents."),
    ).toBeInTheDocument();
    expect(searchApi.searchDocuments).not.toHaveBeenCalled();
  });

  it("shows results for a query and navigates to the document", async () => {
    vi.mocked(searchApi.searchDocuments).mockResolvedValue(page());

    renderWithProviders(<SearchPage />, {
      path: "/search",
      initialEntries: ["/search"],
    });
    submitSearch("balance");

    expect(await screen.findByText("statement.pdf")).toBeInTheDocument();
    expect(searchApi.searchDocuments).toHaveBeenCalledWith({
      q: "balance",
      page: 0,
    });
  });

  it("shows the empty state when nothing matches", async () => {
    vi.mocked(searchApi.searchDocuments).mockResolvedValue(
      page({ content: [] }),
    );

    renderWithProviders(<SearchPage />);
    submitSearch("helicopter");

    expect(
      await screen.findByText(/no documents matched/i),
    ).toBeInTheDocument();
  });

  it("shows an error state when the search fails", async () => {
    vi.mocked(searchApi.searchDocuments).mockRejectedValue(
      new Error("Network"),
    );

    renderWithProviders(<SearchPage />);
    submitSearch("balance");

    expect(
      await screen.findByText(
        "Your search could not be completed. Please try again.",
      ),
    ).toBeInTheDocument();
  });

  it("pages forward when there is a next page", async () => {
    vi.mocked(searchApi.searchDocuments).mockResolvedValue(
      page({ hasNext: true }),
    );

    renderWithProviders(<SearchPage />);
    submitSearch("balance");

    const next = await screen.findByRole("button", { name: "Next" });
    fireEvent.click(next);

    await waitFor(() =>
      expect(searchApi.searchDocuments).toHaveBeenCalledWith({
        q: "balance",
        page: 1,
      }),
    );
  });
});
