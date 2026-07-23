import {
  Alert,
  Button,
  Chip,
  List,
  ListItemButton,
  ListItemText,
  Stack,
  Typography,
} from "@mui/material";
import { useState } from "react";
import { useNavigate } from "react-router-dom";

import { useReports } from "./useReports";

/**
 * The reports list (API_SPEC §13.2; SRS §4.10) — the user's own reports, paged, with loading, empty, and
 * error states. Reuses the app shell, authentication, the shared API client, and shared pagination. A row
 * links to the report detail page. No dashboards, charts, or filters beyond the documented contract.
 */
export function ReportsListPage() {
  const navigate = useNavigate();
  const [page, setPage] = useState(0);
  const { data, isPending, isError } = useReports({ page });

  return (
    <Stack spacing={2} sx={{ maxWidth: 640 }}>
      <Typography variant="h5" component="h1">
        Reports
      </Typography>

      {isPending && <p role="status">Loading reports…</p>}
      {isError && (
        <Alert severity="error">
          Your reports could not be loaded. Please try again.
        </Alert>
      )}

      {data && data.content.length === 0 && (
        <Typography color="text.secondary">
          No reports yet. Generate one from a document.
        </Typography>
      )}

      {data && data.content.length > 0 && (
        <List>
          {data.content.map((report) => (
            <ListItemButton
              key={report.id}
              onClick={() => navigate(`/reports/${report.id}`)}
            >
              <ListItemText
                primary={report.title ?? "Untitled report"}
                secondary={new Date(report.updatedAt).toLocaleString()}
              />
              <Chip label={report.status} size="small" />
            </ListItemButton>
          ))}
        </List>
      )}

      {data && (data.page > 0 || data.hasNext) && (
        <Stack direction="row" spacing={1}>
          <Button
            variant="text"
            disabled={data.page === 0}
            onClick={() => setPage((current) => Math.max(0, current - 1))}
          >
            Previous
          </Button>
          <Button
            variant="text"
            disabled={!data.hasNext}
            onClick={() => setPage((current) => current + 1)}
          >
            Next
          </Button>
        </Stack>
      )}
    </Stack>
  );
}
