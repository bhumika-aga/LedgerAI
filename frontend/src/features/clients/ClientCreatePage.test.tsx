import { fireEvent, screen, waitFor } from "@testing-library/react";
import { AxiosError, AxiosHeaders } from "axios";
import { afterEach, describe, expect, it, vi } from "vitest";

import { renderWithProviders } from "../../test/renderWithProviders";
import { ClientCreatePage } from "./ClientCreatePage";
import * as clientsApi from "./clientsApi";

vi.mock("./clientsApi");

const created: clientsApi.Client = {
  id: "22222222-2222-2222-2222-222222222222",
  name: "Acme Corp",
  contactDetails: null,
  notes: null,
  status: "ACTIVE",
  archivedAt: null,
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

describe("ClientCreatePage", () => {
  afterEach(() => {
    vi.clearAllMocks();
  });

  it("creates a client from the form", async () => {
    vi.mocked(clientsApi.createClient).mockResolvedValue(created);

    renderWithProviders(<ClientCreatePage />);

    fireEvent.change(screen.getByLabelText(/name/i, { selector: "input" }), {
      target: { value: "Acme Corp" },
    });
    fireEvent.click(screen.getByRole("button", { name: /create client/i }));

    await waitFor(() =>
      expect(vi.mocked(clientsApi.createClient).mock.calls[0][0]).toEqual({
        name: "Acme Corp",
        contactDetails: "",
        notes: "",
      }),
    );
  });

  it("blocks submission client-side when the required name is empty", () => {
    renderWithProviders(<ClientCreatePage />);

    fireEvent.click(screen.getByRole("button", { name: /create client/i }));

    expect(screen.getByText("Client name is required.")).toBeInTheDocument();
    expect(clientsApi.createClient).not.toHaveBeenCalled();
  });

  it("shows the server's field-level message on a 422", async () => {
    vi.mocked(clientsApi.createClient).mockRejectedValue(
      validationError("name", "Must be at most 200 characters."),
    );

    renderWithProviders(<ClientCreatePage />);

    fireEvent.change(screen.getByLabelText(/name/i, { selector: "input" }), {
      target: { value: "Acme Corp" },
    });
    fireEvent.click(screen.getByRole("button", { name: /create client/i }));

    expect(
      await screen.findByText("Must be at most 200 characters."),
    ).toBeInTheDocument();
  });

  it("shows a general error when the backend fails without field errors", async () => {
    vi.mocked(clientsApi.createClient).mockRejectedValue(
      new Error("Network Error"),
    );

    renderWithProviders(<ClientCreatePage />);

    fireEvent.change(screen.getByLabelText(/name/i, { selector: "input" }), {
      target: { value: "Acme Corp" },
    });
    fireEvent.click(screen.getByRole("button", { name: /create client/i }));

    expect(
      await screen.findByText(
        "Your changes could not be saved. Please try again.",
      ),
    ).toBeInTheDocument();
  });
});
