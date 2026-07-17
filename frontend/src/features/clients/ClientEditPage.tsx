import { Alert, Stack, Typography } from "@mui/material";
import { useNavigate, useParams } from "react-router-dom";

import { extractValidationErrors } from "../../shared";
import { ClientForm } from "./ClientForm";
import { useClient, useUpdateClient } from "./useClients";

/**
 * Edit-client page (PRD §8.2 AC-2; API_SPEC §7.4). Loads the current values into the form, then submits
 * a partial update; validation failures (VR-004) surface as field-level messages. A non-owned or
 * missing client shows the same not-found state (BR-004, SECURITY §5).
 */
export function ClientEditPage() {
  const { clientId = "" } = useParams();
  const navigate = useNavigate();
  const { data: client, isPending, isError } = useClient(clientId);
  const updateClient = useUpdateClient(clientId);

  if (isPending) {
    return <p role="status">Loading client…</p>;
  }

  if (isError || !client) {
    return <Alert severity="error">This client could not be found.</Alert>;
  }

  return (
    <Stack spacing={2} sx={{ maxWidth: 480 }}>
      <Typography variant="h5" component="h1">
        Edit client
      </Typography>
      <ClientForm
        initialValues={{
          name: client.name,
          contactDetails: client.contactDetails ?? "",
          notes: client.notes ?? "",
        }}
        submitLabel="Save changes"
        pending={updateClient.isPending}
        fieldErrors={extractValidationErrors(updateClient.error)}
        generalError={
          updateClient.isError &&
          Object.keys(extractValidationErrors(updateClient.error)).length === 0
        }
        onCancel={() => navigate(`/clients/${client.id}`)}
        onSubmit={(values) =>
          updateClient.mutate(values, {
            onSuccess: () => navigate(`/clients/${client.id}`),
          })
        }
      />
    </Stack>
  );
}
