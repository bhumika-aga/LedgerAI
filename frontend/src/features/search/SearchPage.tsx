import {
  Alert,
  Button,
  List,
  ListItemButton,
  ListItemText,
  Stack,
  TextField,
  Typography,
} from "@mui/material";
import type { FormEvent } from "react";
import { useState } from "react";
import { useNavigate } from "react-router-dom";

import { extractValidationErrors } from "../../shared";
import { useSearch } from "./useSearch";

/**
 * Global Search page (API_SPEC §14; SRS §4.11) — a keyword box over the user's document content, with
 * loading, empty, and error states and paging (§14.1). Reuses the app shell, authentication, shared API
 * client, shared pagination, and existing document navigation. It adds no filters, suggestions,
 * autocomplete, or highlighting beyond the documented behavior.
 *
 * <p>The query runs on submit (not per keystroke) and is owner-scoped by the server. A result links to the
 * owning document's detail page.
 */
export function SearchPage() {
  const navigate = useNavigate();
  const [input, setInput] = useState("");
  const [activeQuery, setActiveQuery] = useState("");
  const [page, setPage] = useState(0);

  const { data, isPending, isError, error } = useSearch(activeQuery, page);
  const hasQuery = activeQuery.trim().length > 0;
  const queryError = extractValidationErrors(error).q;

  function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setActiveQuery(input);
    setPage(0);
  }

  return (
    <Stack spacing={2} sx={{ maxWidth: 640 }}>
      <Typography variant="h5" component="h1">
        Search
      </Typography>

      <form onSubmit={handleSubmit}>
        <Stack direction="row" spacing={1}>
          <TextField
            fullWidth
            size="small"
            label="Search documents"
            value={input}
            onChange={(event) => setInput(event.target.value)}
          />
          <Button
            type="submit"
            variant="contained"
            disabled={input.trim().length === 0}
          >
            Search
          </Button>
        </Stack>
      </form>

      {!hasQuery && (
        <Typography color="text.secondary">
          Enter a keyword to search your documents.
        </Typography>
      )}

      {hasQuery && isPending && <p role="status">Searching…</p>}

      {hasQuery && isError && (
        <Alert severity="error">
          {queryError ??
            "Your search could not be completed. Please try again."}
        </Alert>
      )}

      {hasQuery && data && data.content.length === 0 && (
        <Typography color="text.secondary">
          No documents matched “{activeQuery}”.
        </Typography>
      )}

      {hasQuery && data && data.content.length > 0 && (
        <List>
          {data.content.map((result) => (
            <ListItemButton
              key={result.documentId}
              onClick={() => navigate(`/documents/${result.documentId}`)}
            >
              <ListItemText
                primary={result.title}
                secondary={result.matchContext || result.snippet}
              />
            </ListItemButton>
          ))}
        </List>
      )}

      {hasQuery && data && (data.page > 0 || data.hasNext) && (
        <Stack direction="row" spacing={1}>
          <Button
            variant="text"
            disabled={data.page === 0}
            onClick={() => setPage((current) => Math.max(0, current - 1))}
          >
            Previous
          </Button>
          <Button
            variant="text"
            disabled={!data.hasNext}
            onClick={() => setPage((current) => current + 1)}
          >
            Next
          </Button>
        </Stack>
      )}
    </Stack>
  );
}
