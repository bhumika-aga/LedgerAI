import { afterEach, describe, expect, it, vi } from "vitest";
import { apiClient } from "../../shared";
import {
  deleteReport,
  generateReport,
  getReport,
  listReports,
  updateReport,
} from "./reportsApi";

const documentId = "33333333-3333-3333-3333-333333333333";
const reportId = "44444444-4444-4444-4444-444444444444";

const report = {
  id: reportId,
  documentId,
  title: "Q4",
  content: "The report body.",
  status: "DRAFT" as const,
  createdAt: "2026-07-23T00:00:00Z",
  updatedAt: "2026-07-23T00:00:00Z",
};

const page = {
  content: [report],
  page: 0,
  size: 20,
  totalElements: 1,
  totalPages: 1,
  hasNext: false,
};

describe("reportsApi", () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("generates a report via POST with an optional title", async () => {
    const postSpy = vi
      .spyOn(apiClient, "post")
      .mockResolvedValue({ data: report });

    await generateReport(documentId, "Q4");

    expect(postSpy).toHaveBeenCalledWith(`/documents/${documentId}/reports`, {
      title: "Q4",
    });
  });

  it("generates without a body when no title is given", async () => {
    const postSpy = vi
      .spyOn(apiClient, "post")
      .mockResolvedValue({ data: report });

    await generateReport(documentId);

    expect(postSpy).toHaveBeenCalledWith(
      `/documents/${documentId}/reports`,
      undefined,
    );
  });

  it("lists reports, passing the query as params", async () => {
    const getSpy = vi.spyOn(apiClient, "get").mockResolvedValue({ data: page });

    await listReports({ documentId, status: "DRAFT" });

    expect(getSpy).toHaveBeenCalledWith("/reports", {
      params: { documentId, status: "DRAFT" },
    });
  });

  it("gets a single report", async () => {
    const getSpy = vi
      .spyOn(apiClient, "get")
      .mockResolvedValue({ data: report });

    await getReport(reportId);

    expect(getSpy).toHaveBeenCalledWith(`/reports/${reportId}`);
  });

  it("updates a report via PATCH", async () => {
    const patchSpy = vi
      .spyOn(apiClient, "patch")
      .mockResolvedValue({ data: { ...report, status: "SAVED" } });

    await updateReport(reportId, { content: "Edited.", status: "SAVED" });

    expect(patchSpy).toHaveBeenCalledWith(`/reports/${reportId}`, {
      content: "Edited.",
      status: "SAVED",
    });
  });

  it("deletes a report via DELETE", async () => {
    const deleteSpy = vi
      .spyOn(apiClient, "delete")
      .mockResolvedValue({ data: undefined });

    await deleteReport(reportId);

    expect(deleteSpy).toHaveBeenCalledWith(`/reports/${reportId}`);
  });
});
