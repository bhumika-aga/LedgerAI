import { afterEach, describe, expect, it, vi } from "vitest";
import { apiClient } from "../../shared";
import { getActivities } from "./activityApi";

const activity = {
  id: "55555555-5555-5555-5555-555555555555",
  actionType: "CLIENT_CREATED" as const,
  summary: 'Created client "Acme"',
  clientId: "22222222-2222-2222-2222-222222222222",
  documentId: null,
  metadata: null,
  createdAt: "2026-07-21T00:00:00Z",
};

const page = {
  content: [activity],
  page: 0,
  size: 20,
  totalElements: 1,
  totalPages: 1,
  hasNext: false,
};

describe("activityApi", () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("gets the timeline, passing the query as params", async () => {
    const getSpy = vi.spyOn(apiClient, "get").mockResolvedValue({ data: page });

    const result = await getActivities({ page: 0, size: 20 });

    expect(getSpy).toHaveBeenCalledWith("/activities", {
      params: { page: 0, size: 20 },
    });
    expect(result.content[0].actionType).toBe("CLIENT_CREATED");
  });

  it("passes clientId for the per-client view", async () => {
    const getSpy = vi.spyOn(apiClient, "get").mockResolvedValue({ data: page });

    await getActivities({ clientId: activity.clientId });

    expect(getSpy).toHaveBeenCalledWith("/activities", {
      params: { clientId: activity.clientId },
    });
  });
});
