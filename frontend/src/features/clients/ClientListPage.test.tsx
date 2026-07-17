import { screen } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { renderWithProviders } from "../../test/renderWithProviders";
import { ClientListPage } from "./ClientListPage";
import * as clientsApi from "./clientsApi";

vi.mock("./clientsApi");

function pageOf(
  clients: clientsApi.Client[],
): clientsApi.Page<clientsApi.Client> {
  return {
    content: clients,
    page: 0,
    size: 20,
    totalElements: clients.length,
    totalPages: 1,
    hasNext: false,
  };
}

const acme: clientsApi.Client = {
  id: "22222222-2222-2222-2222-222222222222",
  name: "Acme Corp",
  contactDetails: "acme@example.com",
  notes: null,
  status: "ACTIVE",
  archivedAt: null,
  createdAt: "2026-07-17T00:00:00Z",
  updatedAt: "2026-07-17T00:00:00Z",
};

describe("ClientListPage", () => {
  afterEach(() => {
    vi.clearAllMocks();
  });

  it("shows a loading state while clients are fetched", () => {
    vi.mocked(clientsApi.listClients).mockReturnValue(new Promise(() => {}));

    renderWithProviders(<ClientListPage />);

    expect(screen.getByRole("status")).toHaveTextContent("Loading clients…");
  });

  it("shows the empty (first-run) state when there are no clients", async () => {
    vi.mocked(clientsApi.listClients).mockResolvedValue(pageOf([]));

    renderWithProviders(<ClientListPage />);

    expect(
      await screen.findByText("You have no clients yet."),
    ).toBeInTheDocument();
  });

  it("lists the clients when there are some", async () => {
    vi.mocked(clientsApi.listClients).mockResolvedValue(pageOf([acme]));

    renderWithProviders(<ClientListPage />);

    expect(await screen.findByText("Acme Corp")).toBeInTheDocument();
    expect(screen.getByText("acme@example.com")).toBeInTheDocument();
  });

  it("shows an error state when the list cannot be loaded", async () => {
    vi.mocked(clientsApi.listClients).mockRejectedValue(
      new Error("Network Error"),
    );

    renderWithProviders(<ClientListPage />);

    expect(
      await screen.findByText(
        "Your clients could not be loaded. Please try again.",
      ),
    ).toBeInTheDocument();
  });
});
