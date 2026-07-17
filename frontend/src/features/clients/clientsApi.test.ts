import { afterEach, describe, expect, it, vi } from "vitest";

import { apiClient } from "../../shared";
import {
  archiveClient,
  createClient,
  getClient,
  listClients,
  updateClient,
} from "./clientsApi";

const client = {
  id: "22222222-2222-2222-2222-222222222222",
  name: "Acme Corp",
  contactDetails: "acme@example.com",
  notes: "Notes",
  status: "ACTIVE" as const,
  archivedAt: null,
  createdAt: "2026-07-17T00:00:00Z",
  updatedAt: "2026-07-17T00:00:00Z",
};

const page = {
  content: [client],
  page: 0,
  size: 20,
  totalElements: 1,
  totalPages: 1,
  hasNext: false,
};

describe("clientsApi", () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("lists clients, passing the query as params", async () => {
    const getSpy = vi.spyOn(apiClient, "get").mockResolvedValue({ data: page });

    const result = await listClients({
      status: "ARCHIVED",
      q: "acme",
      page: 1,
      size: 5,
    });

    expect(getSpy).toHaveBeenCalledWith("/clients", {
      params: { status: "ARCHIVED", q: "acme", page: 1, size: 5 },
    });
    expect(result.totalElements).toBe(1);
    expect(result.content[0].name).toBe("Acme Corp");
  });

  it("lists with no params by default", async () => {
    const getSpy = vi.spyOn(apiClient, "get").mockResolvedValue({ data: page });

    await listClients();

    expect(getSpy).toHaveBeenCalledWith("/clients", { params: {} });
  });

  it("gets a single client by id", async () => {
    const getSpy = vi
      .spyOn(apiClient, "get")
      .mockResolvedValue({ data: client });

    const result = await getClient(client.id);

    expect(getSpy).toHaveBeenCalledWith(`/clients/${client.id}`);
    expect(result.id).toBe(client.id);
  });

  it("creates a client", async () => {
    const postSpy = vi
      .spyOn(apiClient, "post")
      .mockResolvedValue({ data: client });

    await createClient({
      name: "Acme Corp",
      contactDetails: "acme@example.com",
    });

    expect(postSpy).toHaveBeenCalledWith("/clients", {
      name: "Acme Corp",
      contactDetails: "acme@example.com",
    });
  });

  it("updates a client with a partial body", async () => {
    const patchSpy = vi
      .spyOn(apiClient, "patch")
      .mockResolvedValue({ data: client });

    await updateClient(client.id, { notes: "Updated" });

    expect(patchSpy).toHaveBeenCalledWith(`/clients/${client.id}`, {
      notes: "Updated",
    });
  });

  it("archives a client via DELETE", async () => {
    const deleteSpy = vi
      .spyOn(apiClient, "delete")
      .mockResolvedValue({ data: undefined });

    await archiveClient(client.id);

    expect(deleteSpy).toHaveBeenCalledWith(`/clients/${client.id}`);
  });
});
