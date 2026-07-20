import {
  Alert,
  Button,
  CircularProgress,
  Stack,
  TextField,
  Typography,
} from "@mui/material";
import { isAxiosError } from "axios";
import { useEffect, useState } from "react";

import type { DocumentStatus } from "../documents/documentsApi";
import { useEditSummary, useGenerateSummary, useSummary } from "./useSummary";

interface DocumentSummaryPanelProps {
  documentId: string;
  documentStatus: DocumentStatus;
}

/**
 * AI Summary surface for a document (API_SPEC §10; SRS §4.7). Reuses the existing document-detail
 * route, layout, auth, and shared API client (FRONTEND_CODING_STANDARDS §3, §7) — the summary lives on
 * the document it describes rather than a new page.
 *
 * <p>It composes the documented states: gated until the document is {@code READY} (BR-010); an empty
 * "generate" state when no summary exists yet ({@code 404}); an in-flight state honoring the async-ready
 * poll (§2.11); an editable, review-required result (BR-031/032, human-in-the-loop); and a failure state
 * with retry (AI_ARCHITECTURE §12). No chat, email, or report surfaces are present.
 */
export function DocumentSummaryPanel({
  documentId,
  documentStatus,
}: DocumentSummaryPanelProps) {
  const summary = useSummary(documentId);
  const generate = useGenerateSummary(documentId);
  const edit = useEditSummary(documentId);

  // The freshest known summary: a just-saved edit, then a just-generated result, then the loaded one.
  const current = edit.data ?? generate.data ?? summary.data;

  const [draft, setDraft] = useState("");

  // Keep the editable draft in sync with the latest completed content (initial load, regeneration, edit).
  useEffect(() => {
    if (current?.status === "COMPLETED" && current.content !== null) {
      setDraft(current.content);
    }
  }, [current?.status, current?.content]);

  const heading = (
    <Typography variant="subtitle1" component="h2">
      AI summary
    </Typography>
  );

  // A summary can only be produced once text extraction has completed (BR-010).
  if (documentStatus !== "READY") {
    return (
      <Stack spacing={1}>
        {heading}
        <Alert severity="info">
          A summary can be generated once the document is ready.
        </Alert>
      </Stack>
    );
  }

  const generating =
    generate.isPending ||
    current?.status === "REQUESTED" ||
    current?.status === "IN_PROGRESS";

  const notFound =
    !current &&
    !generating &&
    summary.isError &&
    isAxiosError(summary.error) &&
    summary.error.response?.status === 404;

  const loadError = !current && !generating && summary.isError && !notFound;

  return (
    <Stack spacing={1}>
      {heading}

      {summary.isPending && !current && <p role="status">Loading summary…</p>}

      {/* No summary yet (404): offer to generate one. */}
      {notFound && (
        <Stack spacing={1} alignItems="flex-start">
          <Typography>
            No summary has been generated for this document yet.
          </Typography>
          <Button
            variant="contained"
            onClick={() => generate.mutate()}
            disabled={generate.isPending}
          >
            Generate summary
          </Button>
        </Stack>
      )}

      {/* A non-404 load error (distinct from "no summary yet"). */}
      {loadError && (
        <Alert severity="error">The summary could not be loaded.</Alert>
      )}

      {generating && (
        <Stack direction="row" spacing={1} alignItems="center">
          <CircularProgress size={18} />
          <span role="status">Generating summary…</span>
        </Stack>
      )}

      {generate.isError && (
        <Alert severity="error">
          The summary could not be generated. Please try again.
        </Alert>
      )}

      {/* Failed attempt: show the reason and allow a retry (regenerate). */}
      {!generating && current?.status === "FAILED" && (
        <Stack spacing={1} alignItems="flex-start">
          <Alert severity="error">
            {current.failureReason ?? "The summary could not be generated."}
          </Alert>
          <Button variant="outlined" onClick={() => generate.mutate(true)}>
            Try again
          </Button>
        </Stack>
      )}

      {/* Completed: editable, review-required result (human-in-the-loop, BR-031/032). */}
      {!generating && current?.status === "COMPLETED" && (
        <Stack spacing={1}>
          <Typography variant="caption" color="text.secondary">
            AI-generated — review before relying on it.
            {current.edited && " (edited)"}
          </Typography>
          <TextField
            label="Summary"
            multiline
            minRows={4}
            value={draft}
            onChange={(event) => setDraft(event.target.value)}
            slotProps={{ htmlInput: { "aria-label": "Summary" } }}
          />
          {edit.isError && (
            <Alert severity="error">
              The summary could not be saved. Please try again.
            </Alert>
          )}
          <Stack direction="row" spacing={1}>
            <Button
              variant="contained"
              onClick={() => edit.mutate(draft)}
              disabled={edit.isPending || draft.trim().length === 0}
            >
              {edit.isPending ? "Saving…" : "Save"}
            </Button>
            <Button
              variant="text"
              onClick={() => generate.mutate(true)}
              disabled={generate.isPending}
            >
              Regenerate
            </Button>
          </Stack>
        </Stack>
      )}
    </Stack>
  );
}
