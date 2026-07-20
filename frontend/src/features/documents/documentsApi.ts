import type { Page } from "../../shared";
import { apiClient } from "../../shared";

/**
 * Typed client for the Documents module (API_SPEC §8). Reuses the shared `apiClient` (Bearer + cookies)
 * and shared `Page`, describing only the five documented operations (FRONTEND_CODING_STANDARDS §8).
 * There is deliberately no update call: API_SPEC §8 defines no document edit.
 */

export type DocumentStatus =
  | "UPLOADED"
  | "PROCESSING"
  | "OCR_PROCESSING"
  | "READY"
  | "FAILED"
  | "DELETED";

export type ExtractionMethod = "NATIVE" | "OCR";

export type ExtractionQuality = "HIGH" | "LOW" | "UNKNOWN";

/**
 * OCR/processing status (API_SPEC §9.1) — the poll shape for upload/processing (§2.11). It carries no
 * extracted text: no documented endpoint exposes the text itself.
 */
export interface OcrStatus {
  documentId: string;
  status: DocumentStatus;
  extractionMethod: ExtractionMethod | null;
  extractionQuality: ExtractionQuality | null;
  failureReason: string | null;
}

/** DocumentResponse (API_SPEC §17.4). Never carries the internal `storageReference`. */
export interface Document {
  id: string;
  clientId: string;
  originalFilename: string;
  mimeType: string;
  sizeBytes: number;
  status: DocumentStatus;
  extractionMethod: ExtractionMethod | null;
  failureReason: string | null;
  createdAt: string;
  updatedAt: string;
}

/** Download access reference (API_SPEC §8.5) — a short-lived link plus file metadata, not the bytes. */
export interface DocumentDownload {
  downloadUrl: string;
  expiresAt: string;
  mimeType: string;
  originalFilename: string;
  sizeBytes: number;
}

export interface ListDocumentsQuery {
  status?: DocumentStatus;
  page?: number;
  size?: number;
}

export async function listDocuments(
  clientId: string,
  query: ListDocumentsQuery = {},
): Promise<Page<Document>> {
  const { data } = await apiClient.get<Page<Document>>(
    `/clients/${clientId}/documents`,
    { params: query },
  );
  return data;
}

export async function getDocument(documentId: string): Promise<Document> {
  const { data } = await apiClient.get<Document>(`/documents/${documentId}`);
  return data;
}

/** Uploads the file as `multipart/form-data`; axios sets the multipart boundary automatically. */
export async function uploadDocument(
  clientId: string,
  file: File,
): Promise<Document> {
  const form = new FormData();
  form.append("file", file);
  const { data } = await apiClient.post<Document>(
    `/clients/${clientId}/documents`,
    form,
  );
  return data;
}

export async function getDownload(
  documentId: string,
): Promise<DocumentDownload> {
  const { data } = await apiClient.get<DocumentDownload>(
    `/documents/${documentId}/download`,
  );
  return data;
}

/** DELETE is the documented soft-delete (API_SPEC §8.4), not a hard delete. */
export async function deleteDocument(documentId: string): Promise<void> {
  await apiClient.delete(`/documents/${documentId}`);
}

/** API_SPEC §9.1 — the OCR/processing status poll. */
export async function getOcrStatus(documentId: string): Promise<OcrStatus> {
  const { data } = await apiClient.get<OcrStatus>(
    `/documents/${documentId}/ocr-status`,
  );
  return data;
}
