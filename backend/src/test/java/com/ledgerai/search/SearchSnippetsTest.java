package com.ledgerai.search;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link SearchSnippets} — the deterministic snippet/match-context formatting (API_SPEC
 * §17.7). No matching or ranking is done here (that is PostgreSQL's job); these verify only the plain-text
 * presentation, with no highlighting markup.
 */
class SearchSnippetsTest {
    
    @Test
    void snippetIsALeadingExcerptCollapsedToOneLine() {
        String snippet = SearchSnippets.snippet("Balance sheet\n  total   987654");
        
        assertThat(snippet).isEqualTo("Balance sheet total 987654");
    }
    
    @Test
    void snippetTruncatesLongTextWithAnEllipsis() {
        String snippet = SearchSnippets.snippet("x".repeat(500));
        
        assertThat(snippet).endsWith("…");
        assertThat(snippet).hasSizeLessThan(500);
    }
    
    @Test
    void matchContextIsAWindowAroundTheFirstKeyword() {
        String body = "Intro text. " + "filler ".repeat(30) + "the invoice total is 4200. " + "tail ".repeat(30);
        
        String context = SearchSnippets.matchContext(body, "invoice");
        
        assertThat(context).contains("invoice");
        // Windowed, not the whole body, and no highlight markup added.
        assertThat(context).doesNotContain("<b>").doesNotContain("</b>");
        assertThat(context.length()).isLessThan(body.length());
    }
    
    @Test
    void matchContextFallsBackToTheLeadingExcerptWhenNoKeywordIsFound() {
        String body = "A document about balance sheets and totals.";
        
        // The keyword is not present in the (bounded) body → fall back to the leading excerpt.
        assertThat(SearchSnippets.matchContext(body, "nonexistent"))
            .isEqualTo(SearchSnippets.snippet(body));
    }
    
    @Test
    void matchContextIgnoresWebsearchOperatorsWhenLocatingContext() {
        String body = "The quarterly report mentions revenue growth.";
        
        // Quotes / a leading '-' are stripped when locating context (matching itself is the DB's job).
        assertThat(SearchSnippets.matchContext(body, "\"revenue\" -expenses")).contains("revenue");
    }
}
