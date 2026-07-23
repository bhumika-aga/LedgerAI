import type { Page } from "../../shared";
import { apiClient } from "../../shared";

/**
 * Typed client for the Reports module (API_SPEC §13). Reuses the shared `apiClient` (Bearer + cookies) and
 * shared `Page`, describing only the five documented operations (FRONTEND_CODING_STANDARDS §8). Export is a
 * client-side action on the returned content (API_SPEC §13.4) — there is no export endpoint.
 */

export type ReportStatus = "DRAFT" | "SAVED";

/** ReportResponse (API_SPEC §17.6). */
export interface Report {
  id: string;
  documentId: string;
  title: string | null;
  content: string;
  status: ReportStatus;
  createdAt: string;
  updatedAt: string;
}

export interface ListReportsQuery {
  documentId?: string;
  status?: ReportStatus;
  page?: number;
  size?: number;
}

export interface UpdateReportPayload {
  title?: string;
  content?: string;
  status?: ReportStatus;
}

/** API_SPEC §13.1: generate a report from a document (returns a DRAFT). */
export async function generateReport(
  documentId: string,
  title?: string,
): Promise<Report> {
  const { data } = await apiClient.post<Report>(
    `/documents/${documentId}/reports`,
    title ? { title } : undefined,
  );
  return data;
}

/** API_SPEC §13.2: the caller's reports, paged, optionally filtered. */
export async function listReports(
  query: ListReportsQuery = {},
): Promise<Page<Report>> {
  const { data } = await apiClient.get<Page<Report>>("/reports", {
    params: query,
  });
  return data;
}

/** API_SPEC §13.3: a single report. */
export async function getReport(reportId: string): Promise<Report> {
  const { data } = await apiClient.get<Report>(`/reports/${reportId}`);
  return data;
}

/** API_SPEC §13.4: edit/save a report (partial update). */
export async function updateReport(
  reportId: string,
  payload: UpdateReportPayload,
): Promise<Report> {
  const { data } = await apiClient.patch<Report>(
    `/reports/${reportId}`,
    payload,
  );
  return data;
}

/** API_SPEC §13.5: hard-delete a report. */
export async function deleteReport(reportId: string): Promise<void> {
  await apiClient.delete(`/reports/${reportId}`);
}
