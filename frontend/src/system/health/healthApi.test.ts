import { afterEach, describe, expect, it, vi } from "vitest";

import { apiClient } from "../../shared";
import { checkBackendHealth } from "./healthApi";

describe("checkBackendHealth", () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("calls the actuator health endpoint and reports reachable on 200", async () => {
    const getSpy = vi
      .spyOn(apiClient, "get")
      .mockResolvedValue({ status: 200, data: { status: "UP" } });

    const result = await checkBackendHealth();

    expect(result).toBe(true);
    expect(getSpy).toHaveBeenCalledWith(
      "/actuator/health",
      expect.objectContaining({ baseURL: expect.any(String) }),
    );
  });

  it("rejects when the backend cannot be reached", async () => {
    vi.spyOn(apiClient, "get").mockRejectedValue(new Error("Network Error"));

    await expect(checkBackendHealth()).rejects.toThrow();
  });
});
