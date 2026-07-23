import {
  Alert,
  Button,
  Chip,
  Stack,
  TextField,
  Typography,
} from "@mui/material";
import { useEffect, useState } from "react";
import { Link as RouterLink, useNavigate, useParams } from "react-router-dom";

import type { ReportStatus } from "./reportsApi";
import { useDeleteReport, useReport, useUpdateReport } from "./useReports";

/**
 * Report detail (API_SPEC §13.3–13.5; SRS §4.10) — view, edit, save, export, and delete a report. A report
 * the caller does not own returns {@code 404} from the server (BR-004); the UI shows the same not-found
 * state as a genuinely missing one (SECURITY §5). AI-assisted and review-required (BR-032, FR-RPT-005);
 * editable (BR-031, FR-RPT-003).
 *
 * <p>Export (FR-RPT-004) is a client-side action on the returned content (API_SPEC §13.4 — no export
 * endpoint): the content is saved as a plain document file in the browser.
 */
export function ReportDetailPage() {
  const { reportId = "" } = useParams();
  const navigate = useNavigate();
  const { data: report, isPending, isError } = useReport(reportId);
  const update = useUpdateReport(reportId);
  const remove = useDeleteReport();

  const [title, setTitle] = useState("");
  const [content, setContent] = useState("");

  useEffect(() => {
    if (report) {
      setTitle(report.title ?? "");
      setContent(report.content);
    }
  }, [report?.title, report?.content]);

  if (isPending) {
    return <p role="status">Loading report…</p>;
  }

  if (isError || !report) {
    return <Alert severity="error">This report could not be found.</Alert>;
  }

  function handleDownload(name: string, body: string) {
    // Client-side export on the returned content (FR-RPT-004, API_SPEC §13.4) — no export endpoint.
    const blob = new Blob([body], { type: "text/markdown" });
    const url = URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.href = url;
    link.download = `${name || "report"}.md`;
    link.click();
    URL.revokeObjectURL(url);
  }

  const toggledStatus: ReportStatus =
    report.status === "DRAFT" ? "SAVED" : "DRAFT";

  return (
    <Stack spacing={2} sx={{ maxWidth: 640 }}>
      <Stack direction="row" justifyContent="space-between" alignItems="center">
        <Typography variant="h5" component="h1">
          Report
        </Typography>
        <Chip label={report.status} size="small" />
      </Stack>

      <Typography variant="caption" color="text.secondary">
        AI-generated — review before relying on it.
      </Typography>

      <TextField
        label="Title"
        size="small"
        value={title}
        onChange={(event) => setTitle(event.target.value)}
      />
      <TextField
        label="Report"
        multiline
        minRows={8}
        value={content}
        onChange={(event) => setContent(event.target.value)}
        slotProps={{ htmlInput: { "aria-label": "Report" } }}
      />

      {update.isError && (
        <Alert severity="error">
          The report could not be saved. Please try again.
        </Alert>
      )}
      {remove.isError && (
        <Alert severity="error">
          The report could not be deleted. Please try again.
        </Alert>
      )}

      <Stack direction="row" spacing={1} flexWrap="wrap">
        <Button
          variant="contained"
          disabled={update.isPending || content.trim().length === 0}
          onClick={() => update.mutate({ title, content })}
        >
          {update.isPending ? "Saving…" : "Save"}
        </Button>
        <Button
          variant="outlined"
          onClick={() => update.mutate({ status: toggledStatus })}
        >
          {report.status === "DRAFT" ? "Mark as saved" : "Reopen as draft"}
        </Button>
        <Button
          variant="outlined"
          onClick={() =>
            handleDownload(report.title ?? "report", report.content)
          }
        >
          Download
        </Button>
        <Button
          variant="text"
          color="error"
          disabled={remove.isPending}
          onClick={() =>
            remove.mutate(report.id, {
              onSuccess: () => navigate("/reports"),
            })
          }
        >
          {remove.isPending ? "Deleting…" : "Delete"}
        </Button>
        <Button variant="text" component={RouterLink} to="/reports">
          Back to reports
        </Button>
      </Stack>
    </Stack>
  );
}
