import { Alert, Button, Chip, Stack, Typography } from "@mui/material";
import { Link as RouterLink, useNavigate, useParams } from "react-router-dom";

import { useArchiveClient, useClient } from "./useClients";

/**
 * Client workspace / detail (FR-CLNT-002 "open a single Client workspace"; API_SPEC §7.2). Shows the
 * client's fields and offers edit and archive actions. A client the caller does not own returns `404`
 * from the server (BR-004), which surfaces here as the same not-found state as a truly missing one —
 * the UI never distinguishes them, mirroring the API's non-disclosure guarantee (SECURITY §5).
 *
 * <p>Documents, generated outputs, and activity within the client (FR-CLNT-006) are out of this
 * slice's scope; this is the container those will later attach to.
 */
export function ClientDetailPage() {
  const { clientId = "" } = useParams();
  const navigate = useNavigate();
  const { data: client, isPending, isError } = useClient(clientId);
  const archiveClient = useArchiveClient();

  if (isPending) {
    return <p role="status">Loading client…</p>;
  }

  if (isError || !client) {
    return <Alert severity="error">This client could not be found.</Alert>;
  }

  return (
    <Stack spacing={2} sx={{ maxWidth: 640 }}>
      <Stack direction="row" justifyContent="space-between" alignItems="center">
        <Typography variant="h5" component="h1">
          {client.name}
        </Typography>
        <Chip
          label={client.status === "ACTIVE" ? "Active" : "Archived"}
          color={client.status === "ACTIVE" ? "success" : "default"}
          size="small"
        />
      </Stack>

      {client.contactDetails && (
        <Typography>
          <strong>Contact:</strong> {client.contactDetails}
        </Typography>
      )}
      {client.notes && (
        <Typography>
          <strong>Notes:</strong> {client.notes}
        </Typography>
      )}

      {archiveClient.isError && (
        <Alert severity="error">
          This client could not be archived. Please try again.
        </Alert>
      )}

      <Stack direction="row" spacing={1}>
        <Button
          variant="outlined"
          component={RouterLink}
          to={`/clients/${client.id}/edit`}
        >
          Edit
        </Button>
        {client.status === "ACTIVE" && (
          <Button
            variant="text"
            color="error"
            disabled={archiveClient.isPending}
            onClick={() =>
              archiveClient.mutate(client.id, {
                onSuccess: () => navigate("/clients"),
              })
            }
          >
            {archiveClient.isPending ? "Archiving…" : "Archive"}
          </Button>
        )}
        <Button variant="text" component={RouterLink} to="/clients">
          Back to clients
        </Button>
      </Stack>
    </Stack>
  );
}
