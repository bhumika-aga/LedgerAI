package com.ledgerai.search;

import java.util.Arrays;
import java.util.List;

/**
 * Derives the presentational {@code snippet} and {@code matchContext} (API_SPEC §17.7) from a document's
 * extracted-text excerpt and the user's query. Pure, deterministic string handling — no ranking, no
 * highlighting markup, no external calls. Matching itself is done by PostgreSQL full-text search
 * (DATABASE §9); this only formats the human-readable context around the first matched keyword.
 */
final class SearchSnippets {
    
    private static final int SNIPPET_LENGTH = 200;
    private static final int CONTEXT_RADIUS = 80;
    private static final String ELLIPSIS = "…";
    
    private SearchSnippets() {
    }
    
    /**
     * A short, single-line leading excerpt of the extracted text, for preview.
     */
    static String snippet(String body) {
        String normalized = normalize(body);
        if (normalized.length() <= SNIPPET_LENGTH) {
            return normalized;
        }
        return normalized.substring(0, SNIPPET_LENGTH).stripTrailing() + ELLIPSIS;
    }
    
    /**
     * A plain-text window of the extracted text around the first occurrence of any query keyword. Falls
     * back to the leading {@link #snippet(String)} when no keyword is located within the excerpt (e.g. the
     * match lies beyond the bounded excerpt). No markup is inserted.
     */
    static String matchContext(String body, String query) {
        String normalized = normalize(body);
        int matchAt = firstKeywordIndex(normalized, query);
        if (matchAt < 0) {
            return snippet(normalized);
        }
        int start = Math.max(0, matchAt - CONTEXT_RADIUS);
        int end = Math.min(normalized.length(), matchAt + CONTEXT_RADIUS);
        String window = normalized.substring(start, end).strip();
        return (start > 0 ? ELLIPSIS : "") + window + (end < normalized.length() ? ELLIPSIS : "");
    }
    
    private static int firstKeywordIndex(String normalized, String query) {
        String haystack = normalized.toLowerCase();
        int earliest = -1;
        for (String term : keywords(query)) {
            int at = haystack.indexOf(term.toLowerCase());
            if (at >= 0 && (earliest < 0 || at < earliest)) {
                earliest = at;
            }
        }
        return earliest;
    }
    
    /**
     * The plain keywords in a query: split on whitespace, with surrounding quotes and a leading {@code -}
     * (websearch exclusion) stripped, and the boolean word {@code or} dropped. Used only to locate context,
     * not to match — matching is the database's job.
     */
    private static List<String> keywords(String query) {
        if (query == null) {
            return List.of();
        }
        return Arrays.stream(query.trim().split("\\s+"))
                   .map(token -> token.replaceAll("^[-\"']+", "").replaceAll("[\"']+$", ""))
                   .filter(token -> !token.isBlank() && !token.equalsIgnoreCase("or"))
                   .toList();
    }
    
    private static String normalize(String text) {
        return text == null ? "" : text.replaceAll("\\s+", " ").strip();
    }
}
