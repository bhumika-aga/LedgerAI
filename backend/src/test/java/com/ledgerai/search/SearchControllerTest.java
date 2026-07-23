package com.ledgerai.search;

import com.ledgerai.auth.config.SecurityConfig;
import com.ledgerai.common.dto.PageResponse;
import com.ledgerai.common.exception.GlobalExceptionHandler;
import com.ledgerai.common.exception.ValidationFailedException;
import com.ledgerai.common.security.CurrentUserProvider;
import com.ledgerai.search.dto.SearchResultResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web-layer tests for {@link SearchController} (API_SPEC §14): the page envelope (§17.9), the result shape
 * (§17.7), the {@code 422} on an invalid query (VR-006), and the authenticated-only split. The service is
 * mocked.
 */
@WebMvcTest(SearchController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class, CurrentUserProvider.class})
@ActiveProfiles("test")
class SearchControllerTest {
    
    private static final UUID DOC_ID = UUID.randomUUID();
    private static final UUID CLIENT_ID = UUID.randomUUID();
    
    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private SearchService searchService;
    
    private static RequestPostProcessor signedIn() {
        return jwt().jwt(builder -> builder.subject(UUID.randomUUID().toString()));
    }
    
    private PageResponse<SearchResultResponse> onePage() {
        SearchResultResponse hit = new SearchResultResponse(DOC_ID, CLIENT_ID, "statement.pdf",
            "Balance sheet total 987654", "…balance sheet total 987654…", Instant.now());
        return new PageResponse<>(List.of(hit), 0, 20, 1L, 1, false);
    }
    
    @Test
    void searchReturnsThePageEnvelope() throws Exception {
        when(searchService.search(eq("balance"), any())).thenReturn(onePage());
        
        mockMvc.perform(get("/api/v1/search").param("q", "balance").with(signedIn()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].documentId").value(DOC_ID.toString()))
            .andExpect(jsonPath("$.content[0].clientId").value(CLIENT_ID.toString()))
            .andExpect(jsonPath("$.content[0].title").value("statement.pdf"))
            .andExpect(jsonPath("$.content[0].matchContext").exists())
            .andExpect(jsonPath("$.totalElements").value(1));
    }
    
    @Test
    void anInvalidQueryIs422WithAFieldError() throws Exception {
        when(searchService.search(any(), any()))
            .thenThrow(new ValidationFailedException(Map.of("q", "A search query is required.")));
        
        mockMvc.perform(get("/api/v1/search").param("q", "  ").with(signedIn()))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.type").value("/problems/validation-error"))
            .andExpect(jsonPath("$.validationErrors[0].field").value("q"));
    }
    
    @Test
    void searchRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/search").param("q", "balance")).andExpect(status().isUnauthorized());
    }
}
