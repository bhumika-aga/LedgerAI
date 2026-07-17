import {
  Alert,
  Box,
  Button,
  List,
  ListItemButton,
  ListItemText,
  Stack,
  Typography,
} from "@mui/material";
import { Link as RouterLink, useNavigate } from "react-router-dom";

import { useClients } from "./useClients";

/**
 * Client list (PRD §8.2 AC-2; API_SPEC §7.1). Shows the caller's active clients with loading, empty
 * (first-run), and error states, and links into each client's workspace. Ownership is enforced by the
 * server — this only renders what the owner-scoped list returns.
 *
 * <p>Kept intentionally minimal: no dashboard, navigation redesign, or reusable table framework, which
 * are out of this slice's scope. Archived clients are reachable via the documented `status=ARCHIVED`
 * filter but are not surfaced with UI here beyond the default active view.
 */
export function ClientListPage() {
  const navigate = useNavigate();
  const { data, isPending, isError } = useClients();

  return (
    <Stack spacing={2} sx={{ maxWidth: 640 }}>
      <Stack direction="row" justifyContent="space-between" alignItems="center">
        <Typography variant="h5" component="h1">
          Clients
        </Typography>
        <Button variant="contained" component={RouterLink} to="/clients/new">
          New client
        </Button>
      </Stack>

      {isPending && <p role="status">Loading clients…</p>}

      {isError && (
        <Alert severity="error">
          Your clients could not be loaded. Please try again.
        </Alert>
      )}

      {data && data.content.length === 0 && (
        <Box>
          <Typography>You have no clients yet.</Typography>
          <Typography variant="body2" color="text.secondary">
            Create your first client to start organizing your work.
          </Typography>
        </Box>
      )}

      {data && data.content.length > 0 && (
        <List>
          {data.content.map((client) => (
            <ListItemButton
              key={client.id}
              onClick={() => navigate(`/clients/${client.id}`)}
            >
              <ListItemText
                primary={client.name}
                secondary={client.contactDetails ?? undefined}
              />
            </ListItemButton>
          ))}
        </List>
      )}
    </Stack>
  );
}
