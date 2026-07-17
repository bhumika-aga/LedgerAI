import { fireEvent, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { renderWithProviders } from "../../test/renderWithProviders";
import { ClientEditPage } from "./ClientEditPage";
import * as clientsApi from "./clientsApi";

vi.mock("./clientsApi");

const client: clientsApi.Client = {
  id: "22222222-2222-2222-2222-222222222222",
  name: "Acme Corp",
  contactDetails: "acme@example.com",
  notes: "Original notes",
  status: "ACTIVE",
  archivedAt: null,
  createdAt: "2026-07-17T00:00:00Z",
  updatedAt: "2026-07-17T00:00:00Z",
};

function renderEditPage() {
  return renderWithProviders(<ClientEditPage />, {
    path: "/clients/:clientId/edit",
    initialEntries: [`/clients/${client.id}/edit`],
  });
}

describe("ClientEditPage", () => {
  afterEach(() => {
    vi.clearAllMocks();
  });

  it("loads the current values into the form", async () => {
    vi.mocked(clientsApi.getClient).mockResolvedValue(client);

    renderEditPage();

    expect(await screen.findByDisplayValue("Acme Corp")).toBeInTheDocument();
    expect(screen.getByDisplayValue("acme@example.com")).toBeInTheDocument();
    expect(screen.getByDisplayValue("Original notes")).toBeInTheDocument();
    expect(clientsApi.getClient).toHaveBeenCalledWith(client.id);
  });

  it("submits an edit for the client in the route", async () => {
    vi.mocked(clientsApi.getClient).mockResolvedValue(client);
    vi.mocked(clientsApi.updateClient).mockResolvedValue({
      ...client,
      name: "Acme Holdings",
    });

    renderEditPage();

    fireEvent.change(await screen.findByDisplayValue("Acme Corp"), {
      target: { value: "Acme Holdings" },
    });
    fireEvent.click(screen.getByRole("button", { name: /save changes/i }));

    await waitFor(() => expect(clientsApi.updateClient).toHaveBeenCalled());
    const [id, payload] = vi.mocked(clientsApi.updateClient).mock.calls[0];
    expect(id).toBe(client.id);
    expect(payload).toMatchObject({ name: "Acme Holdings" });
  });

  it("shows the not-found state when the client cannot be loaded", async () => {
    // A non-owned client returns 404 from the server (BR-004); the UI does not distinguish it from a
    // genuinely missing one (SECURITY §5).
    vi.mocked(clientsApi.getClient).mockRejectedValue(new Error("Not Found"));

    renderEditPage();

    expect(
      await screen.findByText("This client could not be found."),
    ).toBeInTheDocument();
  });
});
