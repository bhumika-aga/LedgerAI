import { fireEvent, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import type { Page } from "../../shared";
import { renderWithProviders } from "../../test/renderWithProviders";
import * as activityApi from "./activityApi";
import { ActivityTimelinePage } from "./ActivityTimelinePage";

vi.mock("./activityApi");

function page(
  overrides: Partial<Page<activityApi.Activity>> = {},
): Page<activityApi.Activity> {
  return {
    content: [
      {
        id: "55555555-5555-5555-5555-555555555555",
        actionType: "CLIENT_CREATED",
        summary: 'Created client "Acme"',
        clientId: "22222222-2222-2222-2222-222222222222",
        documentId: null,
        metadata: null,
        createdAt: "2026-07-21T00:00:00Z",
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

describe("ActivityTimelinePage", () => {
  afterEach(() => {
    vi.clearAllMocks();
  });

  it("shows a loading state while the timeline loads", () => {
    vi.mocked(activityApi.getActivities).mockReturnValue(new Promise(() => {}));

    renderWithProviders(<ActivityTimelinePage />);

    expect(screen.getByText("Loading activity…")).toBeInTheDocument();
  });

  it("shows the empty state when there is no activity", async () => {
    vi.mocked(activityApi.getActivities).mockResolvedValue(
      page({ content: [] }),
    );

    renderWithProviders(<ActivityTimelinePage />);

    expect(await screen.findByText("No activity yet.")).toBeInTheDocument();
  });

  it("renders the activity entries", async () => {
    vi.mocked(activityApi.getActivities).mockResolvedValue(page());

    renderWithProviders(<ActivityTimelinePage />);

    expect(
      await screen.findByText('Created client "Acme"'),
    ).toBeInTheDocument();
    expect(activityApi.getActivities).toHaveBeenCalledWith({ page: 0 });
  });

  it("shows an error state when the timeline cannot be loaded", async () => {
    vi.mocked(activityApi.getActivities).mockRejectedValue(
      new Error("Network"),
    );

    renderWithProviders(<ActivityTimelinePage />);

    expect(
      await screen.findByText(
        "Your activity could not be loaded. Please try again.",
      ),
    ).toBeInTheDocument();
  });

  it("pages forward when there is a next page", async () => {
    vi.mocked(activityApi.getActivities).mockResolvedValue(
      page({ hasNext: true }),
    );

    renderWithProviders(<ActivityTimelinePage />);

    const next = await screen.findByRole("button", { name: "Next" });
    expect(screen.getByRole("button", { name: "Previous" })).toBeDisabled();

    fireEvent.click(next);

    await waitFor(() =>
      expect(activityApi.getActivities).toHaveBeenCalledWith({ page: 1 }),
    );
  });
});
