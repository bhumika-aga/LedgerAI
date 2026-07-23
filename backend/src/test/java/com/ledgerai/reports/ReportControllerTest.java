package com.ledgerai.reports;

import com.ledgerai.ai.DocumentNotReadyException;
import com.ledgerai.ai.NoExtractableTextException;
import com.ledgerai.ai.port.AiUnavailableException;
import com.ledgerai.auth.config.SecurityConfig;
import com.ledgerai.common.dto.PageResponse;
import com.ledgerai.common.exception.GlobalExceptionHandler;
import com.ledgerai.common.exception.ResourceNotFoundException;
import com.ledgerai.common.exception.ValidationFailedException;
import com.ledgerai.common.security.CurrentUserProvider;
import com.ledgerai.reports.domain.ReportStatus;
import com.ledgerai.reports.dto.ReportResponse;
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
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web-layer tests for {@link ReportController} (API_SPEC §13): the five operations, the documented shapes
 * (§17.6), the authenticated-only split, and the failure mappings routed through the shared handler
 * (404/409/422/503). The service is mocked.
 */
@WebMvcTest(ReportController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class, CurrentUserProvider.class})
@ActiveProfiles("test")
class ReportControllerTest {
    
    private static final UUID DOC_ID = UUID.randomUUID();
    private static final UUID REPORT_ID = UUID.randomUUID();
    
    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private ReportService reportService;
    
    private static RequestPostProcessor signedIn() {
        return jwt().jwt(builder -> builder.subject(UUID.randomUUID().toString()));
    }
    
    private ReportResponse draft() {
        Instant now = Instant.now();
        return new ReportResponse(REPORT_ID, DOC_ID, "Q4", "The report body.", ReportStatus.DRAFT, now, now);
    }
    
    @Test
    void generateReturns201WithADraftReport() throws Exception {
        when(reportService.generate(eq(DOC_ID), any())).thenReturn(draft());
        
        mockMvc.perform(post("/api/v1/documents/{id}/reports", DOC_ID).with(signedIn()))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(REPORT_ID.toString()))
            .andExpect(jsonPath("$.documentId").value(DOC_ID.toString()))
            .andExpect(jsonPath("$.status").value("DRAFT"))
            .andExpect(jsonPath("$.content").value("The report body."));
    }
    
    @Test
    void generateForANonReadyDocumentIs409() throws Exception {
        when(reportService.generate(eq(DOC_ID), any())).thenThrow(new DocumentNotReadyException("not ready"));
        
        mockMvc.perform(post("/api/v1/documents/{id}/reports", DOC_ID).with(signedIn()))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.type").value("/problems/conflict"));
    }
    
    @Test
    void generateWithNoExtractableTextIs422() throws Exception {
        when(reportService.generate(eq(DOC_ID), any())).thenThrow(new NoExtractableTextException("no text"));
        
        mockMvc.perform(post("/api/v1/documents/{id}/reports", DOC_ID).with(signedIn()))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.type").value("/problems/unprocessable-content"));
    }
    
    @Test
    void generateSurfacesProviderUnavailableAs503() throws Exception {
        when(reportService.generate(eq(DOC_ID), any())).thenThrow(new AiUnavailableException("down", null));
        
        mockMvc.perform(post("/api/v1/documents/{id}/reports", DOC_ID).with(signedIn()))
            .andExpect(status().isServiceUnavailable())
            .andExpect(jsonPath("$.type").value("/problems/service-unavailable"));
    }
    
    @Test
    void generateForANonOwnedDocumentIs404() throws Exception {
        when(reportService.generate(eq(DOC_ID), any())).thenThrow(new ResourceNotFoundException());
        
        mockMvc.perform(post("/api/v1/documents/{id}/reports", DOC_ID).with(signedIn()))
            .andExpect(status().isNotFound());
    }
    
    @Test
    void listReturnsThePageEnvelope() throws Exception {
        when(reportService.list(any(), any(), any()))
            .thenReturn(new PageResponse<>(List.of(draft()), 0, 20, 1L, 1, false));
        
        mockMvc.perform(get("/api/v1/reports").with(signedIn()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].id").value(REPORT_ID.toString()))
            .andExpect(jsonPath("$.totalElements").value(1));
    }
    
    @Test
    void getReturnsTheReport() throws Exception {
        when(reportService.get(REPORT_ID)).thenReturn(draft());
        
        mockMvc.perform(get("/api/v1/reports/{id}", REPORT_ID).with(signedIn()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").value("The report body."));
    }
    
    @Test
    void getUnknownOrNonOwnedIs404() throws Exception {
        when(reportService.get(REPORT_ID)).thenThrow(new ResourceNotFoundException());
        
        mockMvc.perform(get("/api/v1/reports/{id}", REPORT_ID).with(signedIn()))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.type").value("/problems/resource-not-found"));
    }
    
    @Test
    void updateReturns200WithTheEditedReport() throws Exception {
        Instant now = Instant.now();
        ReportResponse saved = new ReportResponse(REPORT_ID, DOC_ID, "Q4", "Edited.", ReportStatus.SAVED, now, now);
        when(reportService.update(eq(REPORT_ID), eq("Q4"), eq("Edited."), eq("SAVED"))).thenReturn(saved);
        
        mockMvc.perform(patch("/api/v1/reports/{id}", REPORT_ID)
                            .with(signedIn())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"title\":\"Q4\",\"content\":\"Edited.\",\"status\":\"SAVED\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").value("Edited."))
            .andExpect(jsonPath("$.status").value("SAVED"));
    }
    
    @Test
    void updateWithAnInvalidStatusIs422() throws Exception {
        when(reportService.update(any(), any(), any(), eq("PUBLISHED")))
            .thenThrow(new ValidationFailedException(Map.of("status", "Must be one of DRAFT, SAVED.")));
        
        mockMvc.perform(patch("/api/v1/reports/{id}", REPORT_ID)
                            .with(signedIn())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"status\":\"PUBLISHED\"}"))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.validationErrors[0].field").value("status"));
    }
    
    @Test
    void deleteReturns204() throws Exception {
        mockMvc.perform(delete("/api/v1/reports/{id}", REPORT_ID).with(signedIn()))
            .andExpect(status().isNoContent());
        verify(reportService).delete(REPORT_ID);
    }
    
    @Test
    void deleteUnknownIs404() throws Exception {
        doThrow(new ResourceNotFoundException()).when(reportService).delete(REPORT_ID);
        
        mockMvc.perform(delete("/api/v1/reports/{id}", REPORT_ID).with(signedIn()))
            .andExpect(status().isNotFound());
    }
    
    @Test
    void everyEndpointRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/v1/documents/{id}/reports", DOC_ID)).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/reports")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/reports/{id}", REPORT_ID)).andExpect(status().isUnauthorized());
        mockMvc.perform(patch("/api/v1/reports/{id}", REPORT_ID)
                            .contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/v1/reports/{id}", REPORT_ID)).andExpect(status().isUnauthorized());
    }
}
