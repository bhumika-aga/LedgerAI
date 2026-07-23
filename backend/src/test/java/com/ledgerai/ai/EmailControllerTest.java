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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web-layer tests for {@link EmailController} (API_SPEC §12): the single documented operation, the
 * documented status/shape (§17.5 {@code AIResponse}), the authenticated-only requirement, and the failure
 * mappings routed through the shared handler (404/409/422/503). The service is mocked.
 */
@WebMvcTest(EmailController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class, CurrentUserProvider.class})
@ActiveProfiles("test")
class EmailControllerTest {
    
    private static final UUID EMAIL_ID = UUID.randomUUID();
    private static final UUID DOC_ID = UUID.randomUUID();
    
    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private EmailGenerationService emailGenerationService;
    
    private static RequestPostProcessor signedIn() {
        return jwt().jwt(builder -> builder.subject(UUID.randomUUID().toString()));
    }
    
    private AiResponse drafted() {
        Instant now = Instant.now();
        return new AiResponse(EMAIL_ID, AiRequestType.EMAIL, AiRequestStatus.COMPLETED, DOC_ID,
            "Write a follow-up email.", "Dear client, ...", false, null, now, now);
    }
    
    @Test
    void generateReturns201WithTheDraft() throws Exception {
        when(emailGenerationService.generate(eq("Write a follow-up email."), any(), any()))
            .thenReturn(drafted());
        
        mockMvc.perform(post("/api/v1/ai/emails").with(signedIn())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"instruction\":\"Write a follow-up email.\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(EMAIL_ID.toString()))
            .andExpect(jsonPath("$.type").value("EMAIL"))
            .andExpect(jsonPath("$.status").value("COMPLETED"))
            .andExpect(jsonPath("$.content").value("Dear client, ..."))
            .andExpect(jsonPath("$.edited").value(false));
    }
    
    @Test
    void generatePassesOptionalClientAndDocumentContext() throws Exception {
        UUID clientId = UUID.randomUUID();
        when(emailGenerationService.generate(eq("Draft it."), eq(clientId), eq(DOC_ID)))
            .thenReturn(drafted());
        
        mockMvc.perform(post("/api/v1/ai/emails").with(signedIn())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"instruction\":\"Draft it.\",\"clientId\":\"" + clientId
                                         + "\",\"documentId\":\"" + DOC_ID + "\"}"))
            .andExpect(status().isCreated());
    }
    
    @Test
    void generateWithBlankInstructionIs422WithFieldError() throws Exception {
        mockMvc.perform(post("/api/v1/ai/emails").with(signedIn())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"instruction\":\"\"}"))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.type").value("/problems/validation-error"))
            .andExpect(jsonPath("$.validationErrors[0].field").value("instruction"));
    }
    
    @Test
    void generateForANonOwnedContextIs404() throws Exception {
        when(emailGenerationService.generate(any(), any(), any())).thenThrow(new ResourceNotFoundException());
        
        mockMvc.perform(post("/api/v1/ai/emails").with(signedIn())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"instruction\":\"Draft it.\"}"))
            .andExpect(status().isNotFound());
    }
    
    @Test
    void generateForANonReadyDocumentIs409() throws Exception {
        when(emailGenerationService.generate(any(), any(), any()))
            .thenThrow(new DocumentNotReadyException("not ready"));
        
        mockMvc.perform(post("/api/v1/ai/emails").with(signedIn())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"instruction\":\"Draft it.\"}"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.type").value("/problems/conflict"))
            .andExpect(jsonPath("$.status").value(409));
    }
    
    @Test
    void generateSurfacesProviderUnavailableAs503() throws Exception {
        when(emailGenerationService.generate(any(), any(), any()))
            .thenThrow(new AiUnavailableException("down", null));
        
        mockMvc.perform(post("/api/v1/ai/emails").with(signedIn())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"instruction\":\"Draft it.\"}"))
            .andExpect(status().isServiceUnavailable())
            .andExpect(jsonPath("$.type").value("/problems/service-unavailable"));
    }
    
    @Test
    void generateRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/v1/ai/emails")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"instruction\":\"Draft it.\"}"))
            .andExpect(status().isUnauthorized());
    }
}
