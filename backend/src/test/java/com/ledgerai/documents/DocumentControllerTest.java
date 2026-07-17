package com.ledgerai.documents;

import com.ledgerai.auth.config.SecurityConfig;
import com.ledgerai.common.dto.PageResponse;
import com.ledgerai.common.exception.GlobalExceptionHandler;
import com.ledgerai.common.exception.ResourceNotFoundException;
import com.ledgerai.common.security.CurrentUserProvider;
import com.ledgerai.documents.domain.DocumentStatus;
import com.ledgerai.documents.dto.DocumentDownloadResponse;
import com.ledgerai.documents.dto.DocumentResponse;
import com.ledgerai.documents.port.StorageUnavailableException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web-layer tests for {@link DocumentController} (API_SPEC §8): multipart upload, the documented
 * statuses and shapes (§17.4, §8.5), the authenticated-only split, and the failure mappings routed
 * through the shared handler (404/422/503). The service is mocked.
 */
@WebMvcTest(DocumentController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class, CurrentUserProvider.class})
@ActiveProfiles("test")
class DocumentControllerTest {
    
    private static final UUID CLIENT_ID = UUID.randomUUID();
    private static final UUID DOC_ID = UUID.randomUUID();
    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private DocumentService documentService;
    
    private static RequestPostProcessor signedIn() {
        return jwt().jwt(builder -> builder.subject(UUID.randomUUID().toString()));
    }
    
    private DocumentResponse sampleDocument() {
        Instant now = Instant.now();
        return new DocumentResponse(DOC_ID, CLIENT_ID, "statement.pdf", "application/pdf", 1234L,
            DocumentStatus.UPLOADED, null, null, now, now);
    }
    
    private MockMultipartFile pdf() {
        return new MockMultipartFile("file", "statement.pdf", "application/pdf",
            new byte[]{0x25, 0x50, 0x44, 0x46});
    }
    
    @Test
    void uploadReturns201WithTheDocumentResponse() throws Exception {
        when(documentService.upload(eq(CLIENT_ID), any())).thenReturn(sampleDocument());
        
        mockMvc.perform(multipart("/api/v1/clients/{clientId}/documents", CLIENT_ID).file(pdf()).with(signedIn()))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(DOC_ID.toString()))
            .andExpect(jsonPath("$.clientId").value(CLIENT_ID.toString()))
            .andExpect(jsonPath("$.originalFilename").value("statement.pdf"))
            .andExpect(jsonPath("$.status").value("UPLOADED"))
            .andExpect(jsonPath("$.storageReference").doesNotExist());
    }
    
    @Test
    void uploadWithNoFileIsRejected() throws Exception {
        // Missing the required `file` part → the framework/handler answers 4xx, never 201.
        mockMvc.perform(multipart("/api/v1/clients/{clientId}/documents", CLIENT_ID).with(signedIn()))
            .andExpect(status().is4xxClientError());
    }
    
    @Test
    void uploadSurfacesStorageUnavailableAs503() throws Exception {
        when(documentService.upload(eq(CLIENT_ID), any()))
            .thenThrow(new StorageUnavailableException("down", null));
        
        mockMvc.perform(multipart("/api/v1/clients/{clientId}/documents", CLIENT_ID).file(pdf()).with(signedIn()))
            .andExpect(status().isServiceUnavailable())
            .andExpect(jsonPath("$.type").value("/problems/service-unavailable"))
            .andExpect(jsonPath("$.status").value(503));
    }
    
    @Test
    void uploadToANonOwnedClientIs404() throws Exception {
        when(documentService.upload(eq(CLIENT_ID), any())).thenThrow(new ResourceNotFoundException());
        
        mockMvc.perform(multipart("/api/v1/clients/{clientId}/documents", CLIENT_ID).file(pdf()).with(signedIn()))
            .andExpect(status().isNotFound());
    }
    
    @Test
    void listReturnsThePageEnvelope() throws Exception {
        when(documentService.list(eq(CLIENT_ID), any(), any()))
            .thenReturn(new PageResponse<>(java.util.List.of(sampleDocument()), 0, 20, 1L, 1, false));
        
        mockMvc.perform(get("/api/v1/clients/{clientId}/documents", CLIENT_ID).with(signedIn()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].id").value(DOC_ID.toString()))
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].storageReference").doesNotExist());
    }
    
    @Test
    void getReturnsTheDocument() throws Exception {
        when(documentService.get(DOC_ID)).thenReturn(sampleDocument());
        
        mockMvc.perform(get("/api/v1/documents/{id}", DOC_ID).with(signedIn()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.originalFilename").value("statement.pdf"));
    }
    
    @Test
    void getUnknownOrDeletedIs404() throws Exception {
        when(documentService.get(DOC_ID)).thenThrow(new ResourceNotFoundException());
        
        mockMvc.perform(get("/api/v1/documents/{id}", DOC_ID).with(signedIn()))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.type").value("/problems/resource-not-found"));
    }
    
    @Test
    void downloadReturnsTheAccessReference() throws Exception {
        Instant expiresAt = Instant.now().plusSeconds(300);
        when(documentService.download(DOC_ID)).thenReturn(new DocumentDownloadResponse(
            "https://storage.test/download/ref", expiresAt, "application/pdf", "statement.pdf", 1234L));
        
        mockMvc.perform(get("/api/v1/documents/{id}/download", DOC_ID).with(signedIn()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.downloadUrl").value("https://storage.test/download/ref"))
            .andExpect(jsonPath("$.mimeType").value("application/pdf"))
            .andExpect(jsonPath("$.originalFilename").value("statement.pdf"))
            .andExpect(jsonPath("$.sizeBytes").value(1234));
    }
    
    @Test
    void deleteReturns204() throws Exception {
        mockMvc.perform(delete("/api/v1/documents/{id}", DOC_ID).with(signedIn()))
            .andExpect(status().isNoContent());
        verify(documentService).delete(DOC_ID);
    }
    
    @Test
    void deleteUnknownIs404() throws Exception {
        doThrow(new ResourceNotFoundException()).when(documentService).delete(DOC_ID);
        
        mockMvc.perform(delete("/api/v1/documents/{id}", DOC_ID).with(signedIn()))
            .andExpect(status().isNotFound());
    }
    
    @Test
    void malformedDocumentIdIs400() throws Exception {
        mockMvc.perform(get("/api/v1/documents/{id}", "not-a-uuid").with(signedIn()))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.type").value("/problems/bad-request"));
    }
    
    @Test
    void everyEndpointRequiresAuthentication() throws Exception {
        mockMvc.perform(multipart("/api/v1/clients/{clientId}/documents", CLIENT_ID).file(pdf()))
            .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/clients/{clientId}/documents", CLIENT_ID)).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/documents/{id}", DOC_ID)).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/documents/{id}/download", DOC_ID)).andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/v1/documents/{id}", DOC_ID)).andExpect(status().isUnauthorized());
    }
}
