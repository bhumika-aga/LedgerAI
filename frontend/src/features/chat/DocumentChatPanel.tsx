import {
  Alert,
  Button,
  CircularProgress,
  Stack,
  TextField,
  Typography,
} from "@mui/material";
import { useState } from "react";

import type { DocumentStatus } from "../documents/documentsApi";
import type { ChatExchange } from "./chatApi";
import { useAskQuestion, useChatHistory } from "./useChat";

interface DocumentChatPanelProps {
  documentId: string;
  documentStatus: DocumentStatus;
}

/**
 * AI Chat surface for a document (API_SPEC §11; SRS §4.8). Reuses the existing document-detail route,
 * layout, auth, and shared API client (FRONTEND_CODING_STANDARDS §3, §7) — chat lives on the document it
 * is about rather than a new page, matching the document-scoped MVP model (no conversation resource).
 *
 * <p>It composes the documented states only: gated until the document is {@code READY} (BR-010); an empty
 * thread state; a loading state; the chronological question/answer thread (FR-CHAT-004) with failed
 * attempts shown honestly (AI_ARCHITECTURE §12); a review-required note (BR-032, FR-CHAT-005); and an ask
 * input with an error state. No streaming, markdown rendering, typing indicators, or citations are
 * invented beyond what the documentation specifies.
 */
export function DocumentChatPanel({
  documentId,
  documentStatus,
}: DocumentChatPanelProps) {
  const isReady = documentStatus === "READY";
  // A non-READY document has no thread and cannot be asked about — don't fetch until it is ready (BR-010).
  const history = useChatHistory(documentId, {}, { enabled: isReady });
  const ask = useAskQuestion(documentId);

  const [question, setQuestion] = useState("");

  const heading = (
    <Typography variant="subtitle1" component="h2">
      Ask a question
    </Typography>
  );

  if (!isReady) {
    return (
      <Stack spacing={1}>
        {heading}
        <Alert severity="info">
          You can ask questions once the document is ready.
        </Alert>
      </Stack>
    );
  }

  const exchanges = history.data?.content ?? [];

  function submit() {
    const trimmed = question.trim();
    if (trimmed.length === 0) {
      return;
    }
    ask.mutate(trimmed, { onSuccess: () => setQuestion("") });
  }

  return (
    <Stack spacing={1}>
      {heading}

      <Typography variant="caption" color="text.secondary">
        AI-generated answers, grounded in this document — review before relying
        on them.
      </Typography>

      {history.isPending && <p role="status">Loading conversation…</p>}
      {history.isError && (
        <Alert severity="error">
          The conversation could not be loaded. Please try again.
        </Alert>
      )}

      {history.data && exchanges.length === 0 && (
        <Typography color="text.secondary">
          No questions yet. Ask one below to get a grounded answer.
        </Typography>
      )}

      {exchanges.length > 0 && (
        <Stack
          spacing={2}
          component="ul"
          sx={{ listStyle: "none", pl: 0, m: 0 }}
        >
          {exchanges.map((exchange) => (
            <ChatExchangeItem key={exchange.id} exchange={exchange} />
          ))}
        </Stack>
      )}

      {ask.isError && (
        <Alert severity="error">
          Your question could not be answered. Please try again.
        </Alert>
      )}

      <TextField
        label="Your question"
        multiline
        minRows={2}
        value={question}
        onChange={(event) => setQuestion(event.target.value)}
        slotProps={{ htmlInput: { "aria-label": "Your question" } }}
      />
      <Stack direction="row" spacing={1} alignItems="center">
        <Button
          variant="contained"
          onClick={submit}
          disabled={ask.isPending || question.trim().length === 0}
        >
          {ask.isPending ? "Asking…" : "Ask"}
        </Button>
        {ask.isPending && <CircularProgress size={18} />}
      </Stack>
    </Stack>
  );
}

function ChatExchangeItem({ exchange }: { exchange: ChatExchange }) {
  return (
    <Stack component="li" spacing={0.5}>
      <Typography fontWeight="bold">{exchange.prompt}</Typography>
      {exchange.status === "COMPLETED" && (
        <Typography sx={{ whiteSpace: "pre-wrap" }}>
          {exchange.content}
        </Typography>
      )}
      {exchange.status === "FAILED" && (
        <Alert severity="error">
          {exchange.failureReason ?? "This question could not be answered."}
        </Alert>
      )}
      {(exchange.status === "REQUESTED" ||
        exchange.status === "IN_PROGRESS") && (
        <Typography color="text.secondary">Answering…</Typography>
      )}
    </Stack>
  );
}
