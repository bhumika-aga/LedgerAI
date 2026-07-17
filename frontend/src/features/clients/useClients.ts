import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import type {
  Client,
  CreateClientInput,
  ListClientsQuery,
  Page,
  UpdateClientInput,
} from "./clientsApi";
import {
  archiveClient,
  createClient,
  getClient,
  listClients,
  updateClient,
} from "./clientsApi";

/**
 * Server-state hooks for clients (FRONTEND_CODING_STANDARDS §6, ADR-007). Client data lives in React
 * Query — the single home for server state — never in a global store. Mutations invalidate the list so
 * it reflects the change, and write the returned resource straight into the detail cache to avoid a
 * needless refetch (the API returns the updated `ClientResponse`, API_SPEC §7.3–7.4).
 */

export const clientsKeys = {
  all: ["clients"] as const,
  list: (query: ListClientsQuery) => ["clients", "list", query] as const,
  detail: (clientId: string) => ["clients", "detail", clientId] as const,
};

/** FR-CLNT-002: the caller's clients, filtered and paged. */
export function useClients(query: ListClientsQuery = {}) {
  return useQuery<Page<Client>>({
    queryKey: clientsKeys.list(query),
    queryFn: () => listClients(query),
  });
}

/** FR-CLNT-002: a single client the caller owns. */
export function useClient(clientId: string) {
  return useQuery<Client>({
    queryKey: clientsKeys.detail(clientId),
    queryFn: () => getClient(clientId),
    enabled: Boolean(clientId),
  });
}

/** FR-CLNT-001: create a client. */
export function useCreateClient() {
  const queryClient = useQueryClient();
  return useMutation<Client, unknown, CreateClientInput>({
    mutationFn: createClient,
    onSuccess: (created) => {
      queryClient.setQueryData(clientsKeys.detail(created.id), created);
      void queryClient.invalidateQueries({ queryKey: clientsKeys.all });
    },
  });
}

/** FR-CLNT-003: edit a client. */
export function useUpdateClient(clientId: string) {
  const queryClient = useQueryClient();
  return useMutation<Client, unknown, UpdateClientInput>({
    mutationFn: (input) => updateClient(clientId, input),
    onSuccess: (updated) => {
      queryClient.setQueryData(clientsKeys.detail(updated.id), updated);
      void queryClient.invalidateQueries({ queryKey: clientsKeys.all });
    },
  });
}

/** FR-CLNT-004: archive (soft-deactivate) a client. */
export function useArchiveClient() {
  const queryClient = useQueryClient();
  return useMutation<void, unknown, string>({
    mutationFn: archiveClient,
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: clientsKeys.all });
    },
  });
}
