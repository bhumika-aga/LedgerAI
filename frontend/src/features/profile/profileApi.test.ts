import { AxiosError, AxiosHeaders } from "axios";
import { afterEach, describe, expect, it, vi } from "vitest";

import { apiClient } from "../../shared";
import {
  extractValidationErrors,
  getProfile,
  updateProfile,
} from "./profileApi";

const user = {
  id: "11111111-1111-1111-1111-111111111111",
  email: "pro@example.com",
  fullName: "Ada Pro",
  professionalDetails: "Chartered Accountant",
  preferences: { theme: "dark" },
  createdAt: "2026-07-17T00:00:00Z",
  updatedAt: "2026-07-17T00:00:00Z",
};

function problemResponse(status: number, data: unknown): AxiosError {
  const error = new AxiosError("Request failed");
  error.response = {
    status,
    data,
    statusText: "",
    headers: new AxiosHeaders(),
    config: { headers: new AxiosHeaders() },
  };
  return error;
}

describe("profileApi", () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("gets the caller's profile from /users/me", async () => {
    const getSpy = vi.spyOn(apiClient, "get").mockResolvedValue({ data: user });

    const result = await getProfile();

    expect(getSpy).toHaveBeenCalledWith("/users/me");
    expect(result.email).toBe("pro@example.com");
    expect(result.professionalDetails).toBe("Chartered Accountant");
  });

  it("patches only the supplied fields to /users/me", async () => {
    const patchSpy = vi
      .spyOn(apiClient, "patch")
      .mockResolvedValue({ data: user });

    await updateProfile({ fullName: "Ada Pro" });

    // A partial body — omitted fields must not be sent as nulls (API_SPEC §2.3).
    expect(patchSpy).toHaveBeenCalledWith("/users/me", { fullName: "Ada Pro" });
  });

  it("sends preferences through unchanged", async () => {
    const patchSpy = vi
      .spyOn(apiClient, "patch")
      .mockResolvedValue({ data: user });

    await updateProfile({ preferences: { theme: "dark", nested: { a: 1 } } });

    expect(patchSpy).toHaveBeenCalledWith("/users/me", {
      preferences: { theme: "dark", nested: { a: 1 } },
    });
  });

  it("never sends a user id — the server resolves it from the token", async () => {
    const getSpy = vi.spyOn(apiClient, "get").mockResolvedValue({ data: user });

    await getProfile();

    expect(getSpy).toHaveBeenCalledWith(expect.not.stringContaining(user.id));
  });

  describe("extractValidationErrors", () => {
    it("maps a 422 problem document to field messages", () => {
      const error = problemResponse(422, {
        type: "/problems/validation-error",
        validationErrors: [
          { field: "fullName", message: "Must be at most 255 characters." },
        ],
      });

      expect(extractValidationErrors(error)).toEqual({
        fullName: "Must be at most 255 characters.",
      });
    });

    it("yields no field errors for non-422 failures", () => {
      expect(extractValidationErrors(problemResponse(500, {}))).toEqual({});
      expect(extractValidationErrors(problemResponse(401, {}))).toEqual({});
    });

    it("tolerates a 422 without a usable validationErrors array", () => {
      expect(extractValidationErrors(problemResponse(422, {}))).toEqual({});
      expect(
        extractValidationErrors(
          problemResponse(422, { validationErrors: "nope" }),
        ),
      ).toEqual({});
    });

    it("ignores non-Axios errors", () => {
      expect(extractValidationErrors(new Error("boom"))).toEqual({});
    });
  });
});
