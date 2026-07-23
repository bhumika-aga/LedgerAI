package com.ledgerai.ai;

import com.ledgerai.ai.domain.AiRequestStatus;
import com.ledgerai.ai.domain.AiRequestType;
import com.ledgerai.ai.dto.AiResponse;
import com.ledgerai.ai.port.AiUnavailableException;
import com.ledgerai.auth.config.SecurityConfig;
import com.ledgerai.common.dto.PageResponse;
import com.ledgerai.common.exception.GlobalExceptionHandler;
import com.ledgerai.common.exception.ResourceNotFoundException;
import com.ledgerai.common.security.CurrentUserProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web-layer tests for {@link ChatController} (API_SPEC §11): the two documented operations, the documented
 * statuses/shapes (§17.5 {@code AIResponse}, §17.9 {@code PageResponse}), the authenticated-only split, and
 * the failure mappings routed through the shared handler (404/409/422/503). The service is mocked.
 */
@WebMvcTest(ChatController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class, CurrentUserProvider.class})
@ActiveProfiles("test")
class ChatControllerTest {
    
    private static final UUID DOC_ID = UUID.randomUUID();
    private static final UUID CHAT_ID = UUID.randomUUID();
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockitoBean
    private ChatService chatService;
    
    private static RequestPostProcessor signedIn() {
        return jwt().jwt(builder -> builder.subject(UUID.randomUUID().toString()));
    }
    
    private static String body(String question) {
        return "{\"question\":\"" + question + "\"}";
    }
    
    private AiResponse answered() {
        Instant now = Instant.now();
        return new AiResponse(CHAT_ID, AiRequestType.CHAT, AiRequestStatus.COMPLETED, DOC_ID,
            "What is the total?", "The total is 987654.", false, null, now, now);
    }
    
    @Test
    void askReturns201WithTheAnswer() throws Exception {
        when(chatService.ask(eq(DOC_ID), eq("What is the total?"))).thenReturn(answered());
        
        mockMvc.perform(post("/api/v1/documents/{id}/chat", DOC_ID).with(signedIn())
                            .contentType(MediaType.APPLICATION_JSON).content(body("What is the total?")))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(CHAT_ID.toString()))
            .andExpect(jsonPath("$.type").value("CHAT"))
            .andExpect(jsonPath("$.status").value("COMPLETED"))
            .andExpect(jsonPath("$.documentId").value(DOC_ID.toString()))
            .andExpect(jsonPath("$.prompt").value("What is the total?"))
            .andExpect(jsonPath("$.content").value("The total is 987654."));
    }
    
    @Test
    void askWithBlankQuestionIs422WithFieldError() throws Exception {
        mockMvc.perform(post("/api/v1/documents/{id}/chat", DOC_ID).with(signedIn())
                            .contentType(MediaType.APPLICATION_JSON).content(body("")))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.type").value("/problems/validation-error"))
            .andExpect(jsonPath("$.validationErrors[0].field").value("question"));
    }
    
    @Test
    void askForANonReadyDocumentIs409() throws Exception {
        when(chatService.ask(any(), any())).thenThrow(new DocumentNotReadyException("not ready"));
        
        mockMvc.perform(post("/api/v1/documents/{id}/chat", DOC_ID).with(signedIn())
                            .contentType(MediaType.APPLICATION_JSON).content(body("Anything?")))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.type").value("/problems/conflict"))
            .andExpect(jsonPath("$.status").value(409));
    }
    
    @Test
    void askWithNoExtractableTextIs422() throws Exception {
        when(chatService.ask(any(), any())).thenThrow(new NoExtractableTextException("no text"));
        
        mockMvc.perform(post("/api/v1/documents/{id}/chat", DOC_ID).with(signedIn())
                            .contentType(MediaType.APPLICATION_JSON).content(body("Anything?")))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.type").value("/problems/unprocessable-content"));
    }
    
    @Test
    void askSurfacesProviderUnavailableAs503() throws Exception {
        when(chatService.ask(any(), any())).thenThrow(new AiUnavailableException("down", null));
        
        mockMvc.perform(post("/api/v1/documents/{id}/chat", DOC_ID).with(signedIn())
                            .contentType(MediaType.APPLICATION_JSON).content(body("Anything?")))
            .andExpect(status().isServiceUnavailable())
            .andExpect(jsonPath("$.type").value("/problems/service-unavailable"));
    }
    
    @Test
    void askForANonOwnedDocumentIs404() throws Exception {
        when(chatService.ask(any(), any())).thenThrow(new ResourceNotFoundException());
        
        mockMvc.perform(post("/api/v1/documents/{id}/chat", DOC_ID).with(signedIn())
                            .contentType(MediaType.APPLICATION_JSON).content(body("Anything?")))
            .andExpect(status().isNotFound());
    }
    
    @Test
    void historyReturns200WithThePagedThread() throws Exception {
        PageResponse<AiResponse> page = new PageResponse<>(List.of(answered()), 0, 20, 1, 1, false);
        when(chatService.history(eq(DOC_ID), any(Pageable.class))).thenReturn(page);
        
        mockMvc.perform(get("/api/v1/documents/{id}/chat", DOC_ID).with(signedIn()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].type").value("CHAT"))
            .andExpect(jsonPath("$.content[0].content").value("The total is 987654."))
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.hasNext").value(false));
    }
    
    @Test
    void historyForANonOwnedDocumentIs404() throws Exception {
        when(chatService.history(any(), any())).thenThrow(new ResourceNotFoundException());
        
        mockMvc.perform(get("/api/v1/documents/{id}/chat", DOC_ID).with(signedIn()))
            .andExpect(status().isNotFound());
    }
    
    @Test
    void malformedDocumentIdIs400() throws Exception {
        mockMvc.perform(get("/api/v1/documents/{id}/chat", "not-a-uuid").with(signedIn()))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.type").value("/problems/bad-request"));
    }
    
    @Test
    void everyEndpointRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/v1/documents/{id}/chat", DOC_ID)
                            .contentType(MediaType.APPLICATION_JSON).content(body("Anything?")))
            .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/documents/{id}/chat", DOC_ID)).andExpect(status().isUnauthorized());
    }
}
