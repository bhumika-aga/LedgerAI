import { Alert, Button, Chip, Stack, Typography } from "@mui/material";
import { Link as RouterLink, useNavigate, useParams } from "react-router-dom";

import { getDownload } from "./documentsApi";
import { useDeleteDocument, useDocument } from "./useDocuments";

/**
 * Document detail (API_SPEC §8.3) — metadata, a download action (§8.5), and delete (§8.4). A document
 * the caller does not own returns {@code 404} from the server (BR-004); the UI shows the same
 * not-found state as a genuinely missing one (SECURITY §5).
 *
 * <p>Download follows the external-storage design: it fetches a short-lived access link and opens it,
 * rather than streaming bytes through the API (ADR-008).
 */
export function DocumentDetailPage() {
  const { documentId = "" } = useParams();
  const navigate = useNavigate();
  const { data: document, isPending, isError } = useDocument(documentId);
  const deleteDocument = useDeleteDocument();

  if (isPending) {
    return <p role="status">Loading document…</p>;
  }

  if (isError || !document) {
    return <Alert severity="error">This document could not be found.</Alert>;
  }

  async function handleDownload(id: string) {
    const link = await getDownload(id);
    window.open(link.downloadUrl, "_blank", "noopener");
  }

  return (
    <Stack spacing={2} sx={{ maxWidth: 640 }}>
      <Stack direction="row" justifyContent="space-between" alignItems="center">
        <Typography variant="h5" component="h1">
          {document.originalFilename}
        </Typography>
        <Chip label={document.status} size="small" />
      </Stack>

      <Typography>
        <strong>Type:</strong> {document.mimeType}
      </Typography>
      <Typography>
        <strong>Size:</strong> {document.sizeBytes} bytes
      </Typography>
      {document.failureReason && (
        <Alert severity="error">{document.failureReason}</Alert>
      )}

      {deleteDocument.isError && (
        <Alert severity="error">
          This document could not be deleted. Please try again.
        </Alert>
      )}

      <Stack direction="row" spacing={1}>
        <Button
          variant="outlined"
          onClick={() => void handleDownload(document.id)}
        >
          Download
        </Button>
        <Button
          variant="text"
          color="error"
          disabled={deleteDocument.isPending}
          onClick={() =>
            deleteDocument.mutate(document.id, {
              onSuccess: () =>
                navigate(`/clients/${document.clientId}/documents`),
            })
          }
        >
          {deleteDocument.isPending ? "Deleting…" : "Delete"}
        </Button>
        <Button
          variant="text"
          component={RouterLink}
          to={`/clients/${document.clientId}/documents`}
        >
          Back to documents
        </Button>
      </Stack>
    </Stack>
  );
}
