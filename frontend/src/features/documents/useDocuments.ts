import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import type { Page } from "../../shared";
import type {
  Document,
  DocumentStatus,
  ListDocumentsQuery,
  OcrStatus,
} from "./documentsApi";
import {
  deleteDocument,
  getDocument,
  getOcrStatus,
  listDocuments,
  uploadDocument,
} from "./documentsApi";

/** Lifecycle states where processing is still in flight, so the OCR status should keep polling. */
const IN_FLIGHT_STATUSES: ReadonlySet<DocumentStatus> = new Set([
  "UPLOADED",
  "PROCESSING",
  "OCR_PROCESSING",
]);

/**
 * Server-state hooks for documents (FRONTEND_CODING_STANDARDS §6, ADR-007). Document data lives in
 * React Query, never a global store. Upload and delete invalidate the owning client's document list so
 * it reflects the change; upload also seeds the detail cache from the returned resource.
 */

export const documentsKeys = {
  all: ["documents"] as const,
  list: (clientId: string, query: ListDocumentsQuery) =>
    ["documents", "list", clientId, query] as const,
  detail: (documentId: string) => ["documents", "detail", documentId] as const,
};

/** FR-CLNT-006 / §8.2: a client's documents (excludes soft-deleted server-side). */
export function useDocuments(clientId: string, query: ListDocumentsQuery = {}) {
  return useQuery<Page<Document>>({
    queryKey: documentsKeys.list(clientId, query),
    queryFn: () => listDocuments(clientId, query),
    enabled: Boolean(clientId),
  });
}

/** §8.3: a single non-deleted document the caller owns. */
export function useDocument(documentId: string) {
  return useQuery<Document>({
    queryKey: documentsKeys.detail(documentId),
    queryFn: () => getDocument(documentId),
    enabled: Boolean(documentId),
  });
}

/**
 * API_SPEC §9.1 / §2.11 — poll the OCR/processing status. While the document is still being processed
 * ({@code UPLOADED}/{@code PROCESSING}/{@code OCR_PROCESSING}) it re-fetches on an interval; once the
 * status is terminal ({@code READY}/{@code FAILED}) polling stops. This honors the async-ready contract
 * even though the MVP processes synchronously (ADR-013) and usually returns a terminal status at once.
 */
export function useOcrStatus(documentId: string) {
  return useQuery<OcrStatus>({
    queryKey: [...documentsKeys.detail(documentId), "ocr-status"],
    queryFn: () => getOcrStatus(documentId),
    enabled: Boolean(documentId),
    refetchInterval: (query) => {
      const status = query.state.data?.status;
      return status && IN_FLIGHT_STATUSES.has(status) ? 2000 : false;
    },
  });
}

/** FR-UPLD-001 / §8.1: upload a file to a client. */
export function useUploadDocument(clientId: string) {
  const queryClient = useQueryClient();
  return useMutation<Document, unknown, File>({
    mutationFn: (file) => uploadDocument(clientId, file),
    onSuccess: (created) => {
      queryClient.setQueryData(documentsKeys.detail(created.id), created);
      void queryClient.invalidateQueries({ queryKey: documentsKeys.all });
    },
  });
}

/** FR-STOR-004 / §8.4: soft-delete a document. */
export function useDeleteDocument() {
  const queryClient = useQueryClient();
  return useMutation<void, unknown, string>({
    mutationFn: deleteDocument,
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: documentsKeys.all });
    },
  });
}
