import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { fireEvent, render, screen } from "@testing-library/react";
import { AxiosError, AxiosHeaders } from "axios";
import type { ReactElement } from "react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import * as profileApi from "./profileApi";
import { ProfilePage } from "./ProfilePage";

vi.mock("./profileApi", async () => {
  // Keep the real extractValidationErrors — the page's error rendering depends on its behavior, and
  // stubbing it would test the mock instead of the mapping.
  const actual = await vi.importActual<typeof profileApi>("./profileApi");
  return { ...actual, getProfile: vi.fn(), updateProfile: vi.fn() };
});

const profile = {
  id: "11111111-1111-1111-1111-111111111111",
  email: "pro@example.com",
  fullName: "Ada Pro",
  professionalDetails: "Chartered Accountant",
  preferences: null,
  createdAt: "2026-07-17T00:00:00Z",
  updatedAt: "2026-07-17T00:00:00Z",
};

function validationError(field: string, message: string): AxiosError {
  const error = new AxiosError("Unprocessable Entity");
  error.response = {
    status: 422,
    data: {
      type: "/problems/validation-error",
      validationErrors: [{ field, message }],
    },
    statusText: "",
    headers: new AxiosHeaders(),
    config: { headers: new AxiosHeaders() },
  };
  return error;
}

function renderProfilePage(): ReturnType<typeof render> {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });
  const ui: ReactElement = (
    <QueryClientProvider client={queryClient}>
      <ProfilePage />
    </QueryClientProvider>
  );
  return render(ui);
}

describe("ProfilePage", () => {
  beforeEach(() => {
    vi.mocked(profileApi.getProfile).mockResolvedValue(profile);
  });

  afterEach(() => {
    vi.clearAllMocks();
  });

  it("shows a loading state while the profile is being fetched", () => {
    renderProfilePage();

    expect(screen.getByRole("status")).toHaveTextContent(
      "Loading your profile…",
    );
  });

  it("shows the profile once loaded", async () => {
    renderProfilePage();

    expect(await screen.findByDisplayValue("Ada Pro")).toBeInTheDocument();
    expect(
      screen.getByDisplayValue("Chartered Accountant"),
    ).toBeInTheDocument();
    expect(screen.getByText("pro@example.com")).toBeInTheDocument();
  });

  it("shows an error state when the profile cannot be loaded", async () => {
    vi.mocked(profileApi.getProfile).mockRejectedValue(
      new Error("Network Error"),
    );

    renderProfilePage();

    expect(
      await screen.findByText(
        "Your profile could not be loaded. Please try again.",
      ),
    ).toBeInTheDocument();
  });

  it("saves edited fields and confirms success", async () => {
    vi.mocked(profileApi.updateProfile).mockResolvedValue({
      ...profile,
      fullName: "Ada Professional",
    });
    renderProfilePage();

    fireEvent.change(
      await screen.findByLabelText(/full name/i, { selector: "input" }),
      {
        target: { value: "Ada Professional" },
      },
    );
    fireEvent.click(screen.getByRole("button", { name: /save changes/i }));

    expect(await screen.findByText("Profile updated.")).toBeInTheDocument();
    // Assert on the payload only: React Query also passes its own context as a second argument.
    expect(vi.mocked(profileApi.updateProfile).mock.calls[0][0]).toEqual({
      fullName: "Ada Professional",
      professionalDetails: "Chartered Accountant",
    });
  });

  it("shows the server's field-level message when validation fails", async () => {
    vi.mocked(profileApi.updateProfile).mockRejectedValue(
      validationError("fullName", "Must be at most 255 characters."),
    );
    renderProfilePage();

    await screen.findByDisplayValue("Ada Pro");
    fireEvent.click(screen.getByRole("button", { name: /save changes/i }));

    expect(
      await screen.findByText("Must be at most 255 characters."),
    ).toBeInTheDocument();
    // A field-level failure is not reported as a general one.
    expect(
      screen.queryByText("Your changes could not be saved. Please try again."),
    ).not.toBeInTheDocument();
  });

  it("shows a general error when the backend fails without field errors", async () => {
    vi.mocked(profileApi.updateProfile).mockRejectedValue(
      new Error("Network Error"),
    );
    renderProfilePage();

    await screen.findByDisplayValue("Ada Pro");
    fireEvent.click(screen.getByRole("button", { name: /save changes/i }));

    expect(
      await screen.findByText(
        "Your changes could not be saved. Please try again.",
      ),
    ).toBeInTheDocument();
  });
});
