import { Stack, Typography } from "@mui/material";
import { useNavigate } from "react-router-dom";

import { extractValidationErrors } from "../../shared";
import { ClientForm } from "./ClientForm";
import { useCreateClient } from "./useClients";

/**
 * Create-client page (PRD §8.2 AC-1; API_SPEC §7.3). On success it navigates to the new client's
 * workspace; validation failures (VR-004) surface as field-level messages via the form.
 */
export function ClientCreatePage() {
  const navigate = useNavigate();
  const createClient = useCreateClient();

  return (
    <Stack spacing={2} sx={{ maxWidth: 480 }}>
      <Typography variant="h5" component="h1">
        New client
      </Typography>
      <ClientForm
        submitLabel="Create client"
        pending={createClient.isPending}
        fieldErrors={extractValidationErrors(createClient.error)}
        generalError={
          createClient.isError &&
          Object.keys(extractValidationErrors(createClient.error)).length === 0
        }
        onCancel={() => navigate("/clients")}
        onSubmit={(values) =>
          createClient.mutate(values, {
            onSuccess: (created) => navigate(`/clients/${created.id}`),
          })
        }
      />
    </Stack>
  );
}
