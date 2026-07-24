package com.ledgerai.ai;

import com.ledgerai.ai.domain.AiRequestStatus;
import com.ledgerai.ai.domain.AiRequestType;
import com.ledgerai.ai.dto.AiResponse;
import com.ledgerai.ai.port.AiUnavailableException;
import com.ledgerai.auth.config.SecurityConfig;
import com.ledgerai.common.exception.GlobalExceptionHandler;
import com.ledgerai.common.exception.ResourceNotFoundException;
import com.ledgerai.common.security.CurrentUserProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web-layer tests for {@link AiSummaryController} (API_SPEC §10): the three documented operations, the
 * documented statuses/shapes (§17.5), the authenticated-only split, and the failure mappings routed
 * through the shared handler (404/409/422/503). The service is mocked.
 */
@WebMvcTest(AiSummaryController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class, CurrentUserProvider.class})
@ActiveProfiles("test")
class AiSummaryControllerTest {
    
    private static final UUID DOC_ID = UUID.randomUUID();
    private static final UUID SUMMARY_ID = UUID.randomUUID();
    
    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private AiSummaryService summaryService;
    
    private static RequestPostProcessor signedIn() {
        return jwt().jwt(builder -> builder.subject(UUID.randomUUID().toString()));
    }
    
    private AiResponse completed() {
        Instant now = Instant.now();
        return new AiResponse(SUMMARY_ID, AiRequestType.SUMMARY, AiRequestStatus.COMPLETED, DOC_ID,
            null, "A grounded summary.", false, null, now, now);
    }
    
    @Test
    void generateReturns201WithTheSummary() throws Exception {
        when(summaryService.generate(eq(DOC_ID), anyBoolean())).thenReturn(completed());
        
        mockMvc.perform(post("/api/v1/documents/{id}/summary", DOC_ID).with(signedIn()))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(SUMMARY_ID.toString()))
            .andExpect(jsonPath("$.type").value("SUMMARY"))
            .andExpect(jsonPath("$.status").value("COMPLETED"))
            .andExpect(jsonPath("$.documentId").value(DOC_ID.toString()))
            .andExpect(jsonPath("$.content").value("A grounded summary."))
            .andExpect(jsonPath("$.edited").value(false));
    }
    
    @Test
    void generateForANonReadyDocumentIs409() throws Exception {
        when(summaryService.generate(eq(DOC_ID), anyBoolean()))
            .thenThrow(new DocumentNotReadyException("not ready"));
        
        mockMvc.perform(post("/api/v1/documents/{id}/summary", DOC_ID).with(signedIn()))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.type").value("/problems/conflict"))
            .andExpect(jsonPath("$.status").value(409));
    }
    
    @Test
    void generateWithNoExtractableTextIs422() throws Exception {
        when(summaryService.generate(eq(DOC_ID), anyBoolean()))
            .thenThrow(new NoExtractableTextException("no text"));
        
        mockMvc.perform(post("/api/v1/documents/{id}/summary", DOC_ID).with(signedIn()))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.type").value("/problems/unprocessable-content"))
            .andExpect(jsonPath("$.status").value(422));
    }
    
    @Test
    void generateSurfacesProviderUnavailableAs503() throws Exception {
        when(summaryService.generate(eq(DOC_ID), anyBoolean()))
            .thenThrow(new AiUnavailableException("down", null));
        
        mockMvc.perform(post("/api/v1/documents/{id}/summary", DOC_ID).with(signedIn()))
            .andExpect(status().isServiceUnavailable())
            .andExpect(jsonPath("$.type").value("/problems/service-unavailable"))
            .andExpect(jsonPath("$.status").value(503));
    }
    
    @Test
    void generateForANonOwnedDocumentIs404() throws Exception {
        when(summaryService.generate(eq(DOC_ID), anyBoolean())).thenThrow(new ResourceNotFoundException());
        
        mockMvc.perform(post("/api/v1/documents/{id}/summary", DOC_ID).with(signedIn()))
            .andExpect(status().isNotFound());
    }
    
    @Test
    void getReturnsTheSummary() throws Exception {
        when(summaryService.get(DOC_ID)).thenReturn(completed());
        
        mockMvc.perform(get("/api/v1/documents/{id}/summary", DOC_ID).with(signedIn()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").value("A grounded summary."));
    }
    
    @Test
    void getWhenNoSummaryExistsIs404() throws Exception {
        when(summaryService.get(DOC_ID)).thenThrow(new ResourceNotFoundException());
        
        mockMvc.perform(get("/api/v1/documents/{id}/summary", DOC_ID).with(signedIn()))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.type").value("/problems/resource-not-found"));
    }
    
    @Test
    void editReturns200WithTheEditedSummary() throws Exception {
        Instant now = Instant.now();
        AiResponse edited = new AiResponse(SUMMARY_ID, AiRequestType.SUMMARY, AiRequestStatus.COMPLETED,
            DOC_ID, null, "My edited summary.", true, null, now, now);
        when(summaryService.edit(eq(DOC_ID), eq("My edited summary."))).thenReturn(edited);
        
        mockMvc.perform(patch("/api/v1/documents/{id}/summary", DOC_ID)
                            .with(signedIn())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"content\":\"My edited summary.\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").value("My edited summary."))
            .andExpect(jsonPath("$.edited").value(true));
    }
    
    @Test
    void editWithBlankContentIs422WithFieldError() throws Exception {
        mockMvc.perform(patch("/api/v1/documents/{id}/summary", DOC_ID)
                            .with(signedIn())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"content\":\"\"}"))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.type").value("/problems/validation-error"))
            .andExpect(jsonPath("$.validationErrors[0].field").value("content"));
    }
    
    @Test
    void malformedDocumentIdIs400() throws Exception {
        mockMvc.perform(get("/api/v1/documents/{id}/summary", "not-a-uuid").with(signedIn()))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.type").value("/problems/bad-request"));
    }
    
    @Test
    void everyEndpointRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/v1/documents/{id}/summary", DOC_ID)).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/documents/{id}/summary", DOC_ID)).andExpect(status().isUnauthorized());
        mockMvc.perform(patch("/api/v1/documents/{id}/summary", DOC_ID)
                            .contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"x\"}"))
            .andExpect(status().isUnauthorized());
    }
}
