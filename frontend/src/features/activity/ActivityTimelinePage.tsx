import {
  Alert,
  Button,
  List,
  ListItem,
  ListItemText,
  Stack,
  Typography,
} from "@mui/material";
import { useState } from "react";

import type { ActivityType } from "./activityApi";
import { useActivities } from "./useActivities";

/** Human-readable label for each documented action type (DATABASE §5.8), used when a row has no summary. */
const ACTION_LABELS: Record<ActivityType, string> = {
  CLIENT_CREATED: "Client created",
  DOCUMENT_UPLOADED: "Document uploaded",
  DOCUMENT_DELETED: "Document deleted",
  SUMMARY_GENERATED: "Summary generated",
};

/**
 * The activity timeline (API_SPEC §15; SRS §4.12) — a read-only, chronological (newest-first) list of the
 * user's own activity, with loading, empty, and error states, and paging (API_SPEC §15.1). Owner-scoping
 * is enforced by the server; this renders only what the owner-scoped endpoint returns. It reuses the app
 * shell, authentication, the shared API client, and shared pagination — and adds no filters or actions the
 * documentation does not define (the timeline is not user-mutable, FR-TMLN-004/BR-016).
 */
export function ActivityTimelinePage() {
  const [page, setPage] = useState(0);
  const { data, isPending, isError } = useActivities({ page });

  return (
    <Stack spacing={2} sx={{ maxWidth: 640 }}>
      <Typography variant="h5" component="h1">
        Activity
      </Typography>

      {isPending && <p role="status">Loading activity…</p>}
      {isError && (
        <Alert severity="error">
          Your activity could not be loaded. Please try again.
        </Alert>
      )}

      {data && data.content.length === 0 && (
        <Typography color="text.secondary">No activity yet.</Typography>
      )}

      {data && data.content.length > 0 && (
        <List>
          {data.content.map((activity) => (
            <ListItem key={activity.id} divider>
              <ListItemText
                primary={activity.summary ?? ACTION_LABELS[activity.actionType]}
                secondary={new Date(activity.createdAt).toLocaleString()}
              />
            </ListItem>
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
