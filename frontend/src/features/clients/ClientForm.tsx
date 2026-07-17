import { Alert, Box, Button, Stack, TextField } from "@mui/material";
import type { FormEvent } from "react";
import { useState } from "react";

import type { CreateClientInput } from "./clientsApi";

/**
 * The client create/edit form (FRONTEND_CODING_STANDARDS §4 — Forms). Captures the three editable
 * fields API_SPEC §7.3–7.4 define — name, contact details, notes — and reports field-level validation
 * messages from the server against the right inputs (VR-004).
 *
 * <p>It owns no data fetching: the parent page wires it to the create or update mutation and passes the
 * server's field errors and submit state in. The server is the source of truth for validation, so the
 * only client-side check is that the required name is present — a UX nicety, not the enforcement point
 * (SECURITY §Trust Boundaries).
 */
export interface ClientFormProps {
  initialValues?: CreateClientInput;
  submitLabel: string;
  pending: boolean;
  fieldErrors: Record<string, string>;
  generalError: boolean;
  onSubmit: (values: CreateClientInput) => void;
  onCancel: () => void;
}

export function ClientForm({
  initialValues,
  submitLabel,
  pending,
  fieldErrors,
  generalError,
  onSubmit,
  onCancel,
}: ClientFormProps) {
  const [name, setName] = useState(initialValues?.name ?? "");
  const [contactDetails, setContactDetails] = useState(
    initialValues?.contactDetails ?? "",
  );
  const [notes, setNotes] = useState(initialValues?.notes ?? "");
  const [missingName, setMissingName] = useState(false);

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (name.trim() === "") {
      setMissingName(true);
      return;
    }
    setMissingName(false);
    onSubmit({ name, contactDetails, notes });
  }

  const nameError = missingName ? "Client name is required." : fieldErrors.name;

  return (
    <Box
      component="form"
      onSubmit={handleSubmit}
      noValidate
      sx={{ maxWidth: 480 }}
    >
      <Stack spacing={2}>
        {generalError && (
          <Alert severity="error">
            Your changes could not be saved. Please try again.
          </Alert>
        )}
        <TextField
          label="Name"
          value={name}
          onChange={(event) => setName(event.target.value)}
          error={Boolean(nameError)}
          helperText={nameError}
          required
        />
        <TextField
          label="Contact details"
          value={contactDetails}
          onChange={(event) => setContactDetails(event.target.value)}
          error={Boolean(fieldErrors.contactDetails)}
          helperText={fieldErrors.contactDetails}
          multiline
          minRows={2}
        />
        <TextField
          label="Notes"
          value={notes}
          onChange={(event) => setNotes(event.target.value)}
          error={Boolean(fieldErrors.notes)}
          helperText={fieldErrors.notes}
          multiline
          minRows={3}
        />
        <Stack direction="row" spacing={1}>
          <Button type="submit" variant="contained" disabled={pending}>
            {pending ? "Saving…" : submitLabel}
          </Button>
          <Button
            type="button"
            variant="text"
            onClick={onCancel}
            disabled={pending}
          >
            Cancel
          </Button>
        </Stack>
      </Stack>
    </Box>
  );
}
