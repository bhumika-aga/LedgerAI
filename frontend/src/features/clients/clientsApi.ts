import type { Page } from "../../shared";
import { apiClient } from "../../shared";

/**
 * Typed client for the Clients module (API_SPEC §7). It reuses the shared `apiClient`, which already
 * attaches the Bearer access token and carries credentials, so this layer only describes the five
 * documented operations (FRONTEND_CODING_STANDARDS §8). Types mirror the API_SPEC DTOs (§17.3, §17.9).
 */

export type { Page };

export type ClientStatus = "ACTIVE" | "ARCHIVED";

/** ClientResponse (API_SPEC §17.3). Note there is no `userId` — the contract never exposes the owner. */
export interface Client {
  id: string;
  name: string;
  contactDetails: string | null;
  notes: string | null;
  status: ClientStatus;
  archivedAt: string | null;
  createdAt: string;
  updatedAt: string;
}

/** Create payload (API_SPEC §7.3). */
export interface CreateClientInput {
  name: string;
  contactDetails?: string;
  notes?: string;
}

/** Update payload (API_SPEC §7.4). Every field optional — an omitted one is left unchanged (§2.3). */
export interface UpdateClientInput {
  name?: string;
  contactDetails?: string;
  notes?: string;
}

/** List query (API_SPEC §7.1, §2.5). All optional; the server defaults status to ACTIVE and sorts. */
export interface ListClientsQuery {
  status?: ClientStatus;
  q?: string;
  page?: number;
  size?: number;
}

export async function listClients(
  query: ListClientsQuery = {},
): Promise<Page<Client>> {
  const { data } = await apiClient.get<Page<Client>>("/clients", {
    params: query,
  });
  return data;
}

export async function getClient(clientId: string): Promise<Client> {
  const { data } = await apiClient.get<Client>(`/clients/${clientId}`);
  return data;
}

export async function createClient(input: CreateClientInput): Promise<Client> {
  const { data } = await apiClient.post<Client>("/clients", input);
  return data;
}

export async function updateClient(
  clientId: string,
  input: UpdateClientInput,
): Promise<Client> {
  const { data } = await apiClient.patch<Client>(`/clients/${clientId}`, input);
  return data;
}

/** DELETE is the documented archive action, not a hard delete (API_SPEC §7.5). */
export async function archiveClient(clientId: string): Promise<void> {
  await apiClient.delete(`/clients/${clientId}`);
}
