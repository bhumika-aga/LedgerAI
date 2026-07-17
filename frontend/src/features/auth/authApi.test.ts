import { afterEach, describe, expect, it, vi } from "vitest";

import { apiClient } from "../../shared";
import { getCurrentUser, login, logout, refresh, register } from "./authApi";

const user = {
  id: "11111111-1111-1111-1111-111111111111",
  email: "pro@example.com",
  fullName: "Ada Pro",
  professionalDetails: null,
  preferences: null,
  createdAt: "2026-07-17T00:00:00Z",
  updatedAt: "2026-07-17T00:00:00Z",
};
const tokens = {
  accessToken: "access-token",
  tokenType: "Bearer",
  expiresIn: 900,
};

describe("authApi", () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("posts credentials to /auth/login and returns the user and tokens", async () => {
    const postSpy = vi
      .spyOn(apiClient, "post")
      .mockResolvedValue({ data: { user, tokens } });

    const result = await login({
      email: "pro@example.com",
      password: "correct-horse",
    });

    expect(postSpy).toHaveBeenCalledWith("/auth/login", {
      email: "pro@example.com",
      password: "correct-horse",
    });
    expect(result.tokens.accessToken).toBe("access-token");
    expect(result.user.email).toBe("pro@example.com");
  });

  it("posts the registration payload to /auth/register", async () => {
    const postSpy = vi
      .spyOn(apiClient, "post")
      .mockResolvedValue({ data: { user, tokens } });

    await register({
      email: "pro@example.com",
      password: "correct-horse",
      fullName: "Ada Pro",
    });

    expect(postSpy).toHaveBeenCalledWith("/auth/register", {
      email: "pro@example.com",
      password: "correct-horse",
      fullName: "Ada Pro",
    });
  });

  it("posts to /auth/refresh and unwraps the tokens envelope", async () => {
    const postSpy = vi
      .spyOn(apiClient, "post")
      .mockResolvedValue({ data: { tokens } });

    const result = await refresh();

    expect(postSpy).toHaveBeenCalledWith("/auth/refresh");
    expect(result.accessToken).toBe("access-token");
  });

  it("posts to /auth/logout", async () => {
    const postSpy = vi
      .spyOn(apiClient, "post")
      .mockResolvedValue({ data: undefined });

    await logout();

    expect(postSpy).toHaveBeenCalledWith("/auth/logout");
  });

  it("gets the current user from /auth/me", async () => {
    const getSpy = vi.spyOn(apiClient, "get").mockResolvedValue({ data: user });

    const result = await getCurrentUser();

    expect(getSpy).toHaveBeenCalledWith("/auth/me");
    expect(result.email).toBe("pro@example.com");
  });
});
