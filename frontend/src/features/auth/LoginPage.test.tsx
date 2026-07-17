import { fireEvent, render, screen } from "@testing-library/react";
import type { ReactElement } from "react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import * as authApi from "./authApi";
import { AuthProvider } from "./AuthContext";
import { LoginPage } from "./LoginPage";

vi.mock("./authApi");

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

function renderLoginPage(): ReturnType<typeof render> {
  const ui: ReactElement = (
    <AuthProvider>
      <LoginPage />
    </AuthProvider>
  );
  return render(ui);
}

describe("LoginPage", () => {
  beforeEach(() => {
    // Default bootstrap: no session can be restored, so the form is shown.
    vi.mocked(authApi.refresh).mockRejectedValue(new Error("no session"));
  });

  afterEach(() => {
    vi.clearAllMocks();
  });

  it("shows the sign-in form when no session can be restored", async () => {
    renderLoginPage();

    expect(
      await screen.findByRole("button", { name: /sign in/i }),
    ).toBeInTheDocument();
  });

  it("signs in with valid credentials and shows the signed-in view", async () => {
    vi.mocked(authApi.login).mockResolvedValue({ user, tokens });
    renderLoginPage();

    fireEvent.change(
      await screen.findByLabelText(/email/i, { selector: "input" }),
      {
        target: { value: "pro@example.com" },
      },
    );
    fireEvent.change(
      screen.getByLabelText(/password/i, { selector: "input" }),
      {
        target: { value: "correct-horse" },
      },
    );
    fireEvent.click(screen.getByRole("button", { name: /sign in/i }));

    expect(
      await screen.findByText(/signed in as pro@example.com/i),
    ).toBeInTheDocument();
    expect(authApi.login).toHaveBeenCalledWith({
      email: "pro@example.com",
      password: "correct-horse",
    });
  });

  it("shows a generic, non-revealing error when sign-in fails", async () => {
    vi.mocked(authApi.login).mockRejectedValue(new Error("401 Unauthorized"));
    renderLoginPage();

    fireEvent.change(
      await screen.findByLabelText(/email/i, { selector: "input" }),
      {
        target: { value: "pro@example.com" },
      },
    );
    fireEvent.change(
      screen.getByLabelText(/password/i, { selector: "input" }),
      {
        target: { value: "wrong-password" },
      },
    );
    fireEvent.click(screen.getByRole("button", { name: /sign in/i }));

    expect(
      await screen.findByText("Invalid email or password."),
    ).toBeInTheDocument();
  });
});
