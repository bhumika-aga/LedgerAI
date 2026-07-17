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
import type { ChangeEvent } from "react";
import { useRef } from "react";
import { Link as RouterLink, useNavigate, useParams } from "react-router-dom";

import { extractValidationErrors } from "../../shared";
import { useDocuments, useUploadDocument } from "./useDocuments";

/**
 * A client's documents (FR-CLNT-006; API_SPEC §8.1–8.2) — list with loading, empty (first-run), and
 * error states, and an upload control. Ownership is enforced by the server; this renders only what the
 * owner-scoped list returns, and a non-owned client surfaces as the same not-found state.
 *
 * <p>Upload validation (VR-005) is enforced server-side; a rejected file surfaces the server's
 * field-level message. There is no document edit — API_SPEC §8 defines none.
 */
export function DocumentListPage() {
  const { clientId = "" } = useParams();
  const navigate = useNavigate();
  const fileInput = useRef<HTMLInputElement>(null);
  const { data, isPending, isError } = useDocuments(clientId);
  const uploadDocument = useUploadDocument(clientId);

  const uploadError = extractValidationErrors(uploadDocument.error).file;
  const uploadFailed =
    uploadDocument.isError &&
    Object.keys(extractValidationErrors(uploadDocument.error)).length === 0;

  function handleFileChosen(event: ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0];
    if (file) {
      uploadDocument.mutate(file);
    }
    event.target.value = "";
  }

  return (
    <Stack spacing={2} sx={{ maxWidth: 640 }}>
      <Stack direction="row" justifyContent="space-between" alignItems="center">
        <Typography variant="h5" component="h1">
          Documents
        </Typography>
        <Box>
          <Button
            variant="contained"
            disabled={uploadDocument.isPending}
            onClick={() => fileInput.current?.click()}
          >
            {uploadDocument.isPending ? "Uploading…" : "Upload document"}
          </Button>
          <input
            ref={fileInput}
            type="file"
            aria-label="Upload document"
            hidden
            onChange={handleFileChosen}
          />
        </Box>
      </Stack>

      {uploadDocument.isSuccess && (
        <Alert severity="success">Document uploaded.</Alert>
      )}
      {uploadError && <Alert severity="error">{uploadError}</Alert>}
      {uploadFailed && (
        <Alert severity="error">
          Your document could not be uploaded. Please try again.
        </Alert>
      )}

      {isPending && <p role="status">Loading documents…</p>}
      {isError && (
        <Alert severity="error">
          Your documents could not be loaded. Please try again.
        </Alert>
      )}

      {data && data.content.length === 0 && (
        <Typography color="text.secondary">
          No documents yet. Upload a file to get started.
        </Typography>
      )}

      {data && data.content.length > 0 && (
        <List>
          {data.content.map((document) => (
            <ListItemButton
              key={document.id}
              onClick={() => navigate(`/documents/${document.id}`)}
            >
              <ListItemText
                primary={document.originalFilename}
                secondary={document.status}
              />
            </ListItemButton>
          ))}
        </List>
      )}

      <Button variant="text" component={RouterLink} to={`/clients/${clientId}`}>
        Back to client
      </Button>
    </Stack>
  );
}
