import {
  keepPreviousData,
  useMutation,
  useQuery,
  useQueryClient,
} from "@tanstack/react-query";
import type { Page } from "../../shared";
import type {
  ListReportsQuery,
  Report,
  UpdateReportPayload,
} from "./reportsApi";
import {
  deleteReport,
  generateReport,
  getReport,
  listReports,
  updateReport,
} from "./reportsApi";

/**
 * Server-state hooks for reports (FRONTEND_CODING_STANDARDS §6, ADR-007). Report data lives in React
 * Query, never a global store. Mutations invalidate/seed the relevant caches so the UI reflects changes.
 */

export const reportsKeys = {
  all: ["reports"] as const,
  list: (query: ListReportsQuery) => ["reports", "list", query] as const,
  detail: (reportId: string) => ["reports", "detail", reportId] as const,
};

/**
 * API_SPEC §13.2: the caller's reports, paged, optionally filtered. `enabled` lets a caller (e.g. the
 * document panel) suppress the fetch until it is relevant — a non-READY document has no reports.
 */
export function useReports(
  query: ListReportsQuery = {},
  options: { enabled?: boolean } = {},
) {
  return useQuery<Page<Report>>({
    queryKey: reportsKeys.list(query),
    queryFn: () => listReports(query),
    enabled: options.enabled ?? true,
    placeholderData: keepPreviousData,
  });
}

/** API_SPEC §13.3: a single report. */
export function useReport(reportId: string) {
  return useQuery<Report>({
    queryKey: reportsKeys.detail(reportId),
    queryFn: () => getReport(reportId),
    enabled: Boolean(reportId),
  });
}

/** API_SPEC §13.1: generate a report from a document; invalidates the reports list. */
export function useGenerateReport(documentId: string) {
  const queryClient = useQueryClient();
  return useMutation<Report, unknown, string | void>({
    mutationFn: (title) =>
      generateReport(documentId, typeof title === "string" ? title : undefined),
    onSuccess: (report) => {
      queryClient.setQueryData(reportsKeys.detail(report.id), report);
      void queryClient.invalidateQueries({ queryKey: reportsKeys.all });
    },
  });
}

/** API_SPEC §13.4: edit/save a report; seeds the detail cache and invalidates lists. */
export function useUpdateReport(reportId: string) {
  const queryClient = useQueryClient();
  return useMutation<Report, unknown, UpdateReportPayload>({
    mutationFn: (payload) => updateReport(reportId, payload),
    onSuccess: (report) => {
      queryClient.setQueryData(reportsKeys.detail(report.id), report);
      void queryClient.invalidateQueries({ queryKey: reportsKeys.all });
    },
  });
}

/** API_SPEC §13.5: delete a report; invalidates the reports list. */
export function useDeleteReport() {
  const queryClient = useQueryClient();
  return useMutation<void, unknown, string>({
    mutationFn: (reportId) => deleteReport(reportId),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: reportsKeys.all });
    },
  });
}
