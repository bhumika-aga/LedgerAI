import { Alert, Button, Chip, Divider, Stack, Typography } from "@mui/material";
import { Link as RouterLink, useNavigate, useParams } from "react-router-dom";

import { DocumentSummaryPanel } from "../ai";
import { DocumentReportsPanel } from "../reports";
import { getDownload } from "./documentsApi";
import { useDeleteDocument, useDocument, useOcrStatus } from "./useDocuments";

/** Human-readable label for each lifecycle state (SRS §7.1). */
const STATUS_LABELS: Record<string, string> = {
  UPLOADED: "Uploaded",
  PROCESSING: "Processing…",
  OCR_PROCESSING: "Extracting text…",
  READY: "Ready",
  FAILED: "Failed",
};

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
  const ocr = useOcrStatus(documentId);
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

      {/* OCR / text-extraction status (API_SPEC §9.1). No extracted-text viewer: no endpoint exposes
          the text. */}
      <Stack spacing={1}>
        <Typography variant="subtitle1" component="h2">
          Text extraction
        </Typography>
        {ocr.isPending && <p role="status">Checking extraction status…</p>}
        {ocr.isError && (
          <Alert severity="error">
            The extraction status could not be loaded.
          </Alert>
        )}
        {ocr.data && (
          <Stack spacing={1}>
            <Typography>
              <strong>Status:</strong>{" "}
              {STATUS_LABELS[ocr.data.status] ?? ocr.data.status}
            </Typography>
            {ocr.data.status === "READY" && (
              <>
                <Typography>
                  <strong>Method:</strong>{" "}
                  {ocr.data.extractionMethod === "NATIVE"
                    ? "Embedded text"
                    : "Scanned (OCR)"}
                </Typography>
                {ocr.data.extractionQuality && (
                  <Typography>
                    <strong>Quality:</strong> {ocr.data.extractionQuality}
                    {ocr.data.extractionQuality === "LOW" && (
                      <> — the extraction may be unreliable.</>
                    )}
                  </Typography>
                )}
              </>
            )}
            {ocr.data.status === "FAILED" && (
              <Alert severity="error">
                {ocr.data.failureReason ??
                  "Text could not be extracted from this document."}
              </Alert>
            )}
          </Stack>
        )}
      </Stack>

      <Divider />

      {/* AI summary (API_SPEC §10). Lives on the document it describes; the panel gates itself until the
          document is READY (BR-010). */}
      <DocumentSummaryPanel
        documentId={document.id}
        documentStatus={document.status}
      />

      <Divider />

      {/* Reports (API_SPEC §13). Generated from this document; the panel gates itself until READY (BR-010). */}
      <DocumentReportsPanel
        documentId={document.id}
        documentStatus={document.status}
      />

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
