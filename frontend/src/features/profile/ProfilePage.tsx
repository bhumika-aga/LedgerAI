import {
  Alert,
  Box,
  Button,
  Stack,
  TextField,
  Typography,
} from "@mui/material";
import type { FormEvent } from "react";
import { useEffect, useState } from "react";

import { extractValidationErrors } from "./profileApi";
import { useProfile, useUpdateProfile } from "./useProfile";

/**
 * Profile page (PRD §8.2, SRS §4.2, API_SPEC §6).
 *
 * Reads the caller's own profile and lets them edit the fields PRD §8.2 names — name and professional
 * details — with clear success and error feedback (AC-2). Field-level messages come from the server's
 * `422` (VR-003): its limits are configuration, so mirroring them here would drift.
 *
 * Preferences are intentionally not surfaced: the API and stored blob support them (FR-PROF-005 is a
 * SHOULD), but no document defines a single preference key, and inventing one would be a product
 * decision. A later slice can add the controls additively once the keys exist.
 */
export function ProfilePage() {
  const { data: profile, isPending, isError } = useProfile();
  const updateProfile = useUpdateProfile();

  const [fullName, setFullName] = useState("");
  const [professionalDetails, setProfessionalDetails] = useState("");
  const [seeded, setSeeded] = useState(false);

  useEffect(() => {
    // Seed the form once, so a background refetch can never overwrite what the user is typing.
    if (profile && !seeded) {
      setFullName(profile.fullName ?? "");
      setProfessionalDetails(profile.professionalDetails ?? "");
      setSeeded(true);
    }
  }, [profile, seeded]);

  if (isPending) {
    return <p role="status">Loading your profile…</p>;
  }

  if (isError || !profile) {
    return (
      <Alert severity="error">
        Your profile could not be loaded. Please try again.
      </Alert>
    );
  }

  const fieldErrors = extractValidationErrors(updateProfile.error);
  const failedWithoutFieldErrors =
    updateProfile.isError && Object.keys(fieldErrors).length === 0;

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    updateProfile.mutate({ fullName, professionalDetails });
  }

  return (
    <Box
      component="form"
      onSubmit={handleSubmit}
      noValidate
      sx={{ maxWidth: 480 }}
    >
      <Stack spacing={2}>
        <Typography variant="h5" component="h1">
          Profile
        </Typography>

        <Typography variant="body2">{profile.email}</Typography>

        {updateProfile.isSuccess && (
          <Alert severity="success">Profile updated.</Alert>
        )}
        {failedWithoutFieldErrors && (
          <Alert severity="error">
            Your changes could not be saved. Please try again.
          </Alert>
        )}

        <TextField
          label="Full name"
          value={fullName}
          onChange={(event) => setFullName(event.target.value)}
          error={Boolean(fieldErrors.fullName)}
          helperText={fieldErrors.fullName}
        />
        <TextField
          label="Professional details"
          value={professionalDetails}
          onChange={(event) => setProfessionalDetails(event.target.value)}
          error={Boolean(fieldErrors.professionalDetails)}
          helperText={fieldErrors.professionalDetails}
          multiline
          minRows={3}
        />

        <Button
          type="submit"
          variant="contained"
          disabled={updateProfile.isPending}
        >
          {updateProfile.isPending ? "Saving…" : "Save changes"}
        </Button>
      </Stack>
    </Box>
  );
}
