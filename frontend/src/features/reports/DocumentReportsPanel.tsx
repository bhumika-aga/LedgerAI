import {
  Alert,
  Button,
  Chip,
  CircularProgress,
  List,
  ListItemButton,
  ListItemText,
  Stack,
  Typography,
} from "@mui/material";
import { useNavigate } from "react-router-dom";

import type { DocumentStatus } from "../documents/documentsApi";
import { useGenerateReport, useReports } from "./useReports";

interface DocumentReportsPanelProps {
  documentId: string;
  documentStatus: DocumentStatus;
}

/**
 * Reports surface for a document (API_SPEC §13; SRS §4.10). Lives on the document it describes, reusing the
 * existing document-detail route, layout, auth, and shared API client. It lists the document's existing
 * reports and offers generation, gated until the document is {@code READY} (BR-010/BR-035). Generating a
 * report navigates to the new report's detail page (edit/save/export/delete). No dashboards or charts.
 */
export function DocumentReportsPanel({
  documentId,
  documentStatus,
}: DocumentReportsPanelProps) {
  const navigate = useNavigate();
  // A non-READY document has no reports and cannot generate one — don't fetch until it is ready (BR-010).
  const reports = useReports(
    { documentId },
    { enabled: documentStatus === "READY" },
  );
  const generate = useGenerateReport(documentId);

  const heading = (
    <Typography variant="subtitle1" component="h2">
      Reports
    </Typography>
  );

  if (documentStatus !== "READY") {
    return (
      <Stack spacing={1}>
        {heading}
        <Alert severity="info">
          A report can be generated once the document is ready.
        </Alert>
      </Stack>
    );
  }

  return (
    <Stack spacing={1}>
      {heading}

      {reports.isPending && <p role="status">Loading reports…</p>}
      {reports.isError && (
        <Alert severity="error">
          The reports could not be loaded. Please try again.
        </Alert>
      )}

      {reports.data && reports.data.content.length === 0 && (
        <Typography color="text.secondary">
          No reports for this document yet.
        </Typography>
      )}

      {reports.data && reports.data.content.length > 0 && (
        <List dense>
          {reports.data.content.map((report) => (
            <ListItemButton
              key={report.id}
              onClick={() => navigate(`/reports/${report.id}`)}
            >
              <ListItemText primary={report.title ?? "Untitled report"} />
              <Chip label={report.status} size="small" />
            </ListItemButton>
          ))}
        </List>
      )}

      {generate.isError && (
        <Alert severity="error">
          The report could not be generated. Please try again.
        </Alert>
      )}

      <Stack direction="row" spacing={1} alignItems="center">
        <Button
          variant="contained"
          disabled={generate.isPending}
          onClick={() =>
            generate.mutate(undefined, {
              onSuccess: (report) => navigate(`/reports/${report.id}`),
            })
          }
        >
          Generate report
        </Button>
        {generate.isPending && <CircularProgress size={18} />}
      </Stack>
    </Stack>
  );
}
