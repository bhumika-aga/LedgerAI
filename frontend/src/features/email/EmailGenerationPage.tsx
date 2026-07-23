import {
  Alert,
  Button,
  CircularProgress,
  Stack,
  TextField,
  Typography,
} from "@mui/material";
import { useEffect, useState } from "react";
import { useSearchParams } from "react-router-dom";
import { useGenerateEmail } from "./useEmail";

/**
 * AI Email generation surface (API_SPEC §12; SRS §4.9). A standalone page — email is not nested under a
 * document because its context is optional. It reuses the shared API client, layout, and auth. Optional
 * client/document context is read from the query string (e.g. a document's "Draft an email" action passes
 * {@code ?documentId=…}); when absent, the draft is generated from the instruction alone.
 *
 * <p>It composes the documented states only: an empty state before generation; a loading state; an
 * editable, review-required draft (BR-031/BR-032) that is <strong>never sent</strong> (BR-034,
 * FR-EMAIL-005); and an error state. No mailbox, drafts management, delivery status, scheduling, or rich
 * editor is invented beyond the documentation.
 */
export function EmailGenerationPage() {
  const [searchParams] = useSearchParams();
  const documentId = searchParams.get("documentId") ?? undefined;
  const clientId = searchParams.get("clientId") ?? undefined;

  const generate = useGenerateEmail();
  const [instruction, setInstruction] = useState("");
  const [draft, setDraft] = useState("");

  const result = generate.data;

  // Seed the editable draft from a freshly completed generation (human-in-the-loop, BR-031).
  useEffect(() => {
    if (result?.status === "COMPLETED" && result.content !== null) {
      setDraft(result.content);
    }
  }, [result?.status, result?.content]);

  function submit() {
    const trimmed = instruction.trim();
    if (trimmed.length === 0) {
      return;
    }
    generate.mutate({ instruction: trimmed, documentId, clientId });
  }

  return (
    <Stack spacing={2} sx={{ maxWidth: 640 }}>
      <Typography variant="h5" component="h1">
        Draft an email
      </Typography>
      <Typography variant="caption" color="text.secondary">
        AI-generated draft — review and edit before use. LedgerAI never sends
        email on your behalf.
      </Typography>

      {documentId && (
        <Alert severity="info">This document will be used as context.</Alert>
      )}

      <TextField
        label="Instruction"
        multiline
        minRows={3}
        value={instruction}
        onChange={(event) => setInstruction(event.target.value)}
        slotProps={{ htmlInput: { "aria-label": "Instruction" } }}
      />
      <Stack direction="row" spacing={1} alignItems="center">
        <Button
          variant="contained"
          onClick={submit}
          disabled={generate.isPending || instruction.trim().length === 0}
        >
          {generate.isPending ? "Generating…" : "Generate draft"}
        </Button>
        {generate.isPending && <CircularProgress size={18} />}
      </Stack>

      {generate.isError && (
        <Alert severity="error">
          The email draft could not be generated. Please try again.
        </Alert>
      )}

      {/* Empty state: nothing generated yet and not in flight. */}
      {!result && !generate.isPending && !generate.isError && (
        <Typography color="text.secondary">
          Enter an instruction and generate a draft to see it here.
        </Typography>
      )}

      {/* A recorded failure (e.g. the model returned nothing usable). */}
      {result?.status === "FAILED" && (
        <Alert severity="error">
          {result.failureReason ?? "The email draft could not be generated."}
        </Alert>
      )}

      {/* Completed: an editable, review-required draft. */}
      {result?.status === "COMPLETED" && (
        <Stack spacing={1}>
          <Typography variant="subtitle1" component="h2">
            Draft
          </Typography>
          <TextField
            label="Email draft"
            multiline
            minRows={8}
            value={draft}
            onChange={(event) => setDraft(event.target.value)}
            slotProps={{ htmlInput: { "aria-label": "Email draft" } }}
          />
        </Stack>
      )}
    </Stack>
  );
}
