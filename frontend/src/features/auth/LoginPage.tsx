import {
  Alert,
  Box,
  Button,
  Stack,
  TextField,
  Typography,
} from "@mui/material";
import type { FormEvent } from "react";
import { useState } from "react";

import { useAuth } from "./AuthContext";

/**
 * Sign-in surface for the Authentication slice (SRS §4.1, API_SPEC §5.2).
 *
 * While the session is being restored it shows a neutral loading line; once resolved it renders
 * either the credential form (unauthenticated) or a minimal signed-in view with a sign-out action.
 * The failure message is deliberately generic and never reveals whether the email exists (BR-020).
 */
export function LoginPage() {
  const { status, user, login, logout } = useAuth();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  if (status === "loading") {
    return <p role="status">Restoring session…</p>;
  }

  if (status === "authenticated" && user) {
    return (
      <Stack spacing={2} sx={{ maxWidth: 360 }}>
        <Typography variant="h5" component="h1">
          Signed in
        </Typography>
        <Typography role="status">Signed in as {user.email}</Typography>
        <Button variant="outlined" onClick={() => void logout()}>
          Sign out
        </Button>
      </Stack>
    );
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      await login({ email, password });
    } catch {
      setError("Invalid email or password.");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <Box
      component="form"
      onSubmit={handleSubmit}
      noValidate
      sx={{ maxWidth: 360 }}
    >
      <Stack spacing={2}>
        <Typography variant="h5" component="h1">
          Sign in
        </Typography>
        {error && <Alert severity="error">{error}</Alert>}
        <TextField
          label="Email"
          type="email"
          value={email}
          onChange={(event) => setEmail(event.target.value)}
          autoComplete="email"
          required
        />
        <TextField
          label="Password"
          type="password"
          value={password}
          onChange={(event) => setPassword(event.target.value)}
          autoComplete="current-password"
          required
        />
        <Button type="submit" variant="contained" disabled={submitting}>
          {submitting ? "Signing in…" : "Sign in"}
        </Button>
      </Stack>
    </Box>
  );
}
