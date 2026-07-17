package com.ledgerai.clients;

import com.ledgerai.auth.config.SecurityConfig;
import com.ledgerai.clients.domain.ClientStatus;
import com.ledgerai.clients.dto.ClientResponse;
import com.ledgerai.clients.dto.CreateClientRequest;
import com.ledgerai.clients.dto.UpdateClientRequest;
import com.ledgerai.common.dto.PageResponse;
import com.ledgerai.common.exception.GlobalExceptionHandler;
import com.ledgerai.common.exception.ResourceNotFoundException;
import com.ledgerai.common.exception.ValidationFailedException;
import com.ledgerai.common.security.CurrentUserProvider;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web-layer tests for {@link ClientController} (API_SPEC §7): the documented statuses, the
 * {@code ClientResponse}/{@code PageResponse} shapes (§17.3, §17.9), the pagination contract (§2.5),
 * and the authenticated-only split — with the service mocked.
 */
@WebMvcTest(ClientController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class, CurrentUserProvider.class})
@ActiveProfiles("test")
class ClientControllerTest {
    
    private static final UUID CLIENT_ID = UUID.randomUUID();
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockitoBean
    private ClientService clientService;
    
    private static org.springframework.test.web.servlet.request.RequestPostProcessor signedIn() {
        return jwt().jwt(builder -> builder.subject(UUID.randomUUID().toString()));
    }
    
    private ClientResponse sampleClient() {
        Instant now = Instant.now();
        return new ClientResponse(CLIENT_ID, "Acme Corp", "acme@example.com", "Notes",
            ClientStatus.ACTIVE, null, now, now);
    }
    
    @Test
    void listReturnsThePageResponseEnvelope() throws Exception {
        when(clientService.list(any(), any(), any()))
            .thenReturn(new PageResponse<>(List.of(sampleClient()), 0, 20, 1L, 1, false));
        
        mockMvc.perform(get("/api/v1/clients").with(signedIn()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].id").value(CLIENT_ID.toString()))
            .andExpect(jsonPath("$.content[0].name").value("Acme Corp"))
            .andExpect(jsonPath("$.content[0].status").value("ACTIVE"))
            .andExpect(jsonPath("$.content[0].userId").doesNotExist())
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.size").value(20))
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.totalPages").value(1))
            .andExpect(jsonPath("$.hasNext").value(false))
            // Spring's own Page fields must never leak (API_SPEC §2.12).
            .andExpect(jsonPath("$.pageable").doesNotExist())
            .andExpect(jsonPath("$.numberOfElements").doesNotExist());
    }
    
    @Test
    void listDefaultsToPageZeroSizeTwentySortedByNameAscending() throws Exception {
        when(clientService.list(any(), any(), any()))
            .thenReturn(new PageResponse<>(List.of(), 0, 20, 0L, 0, false));
        
        mockMvc.perform(get("/api/v1/clients").with(signedIn())).andExpect(status().isOk());
        
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(clientService).list(isNull(), isNull(), captor.capture());
        Pageable pageable = captor.getValue();
        assertThat(pageable.getPageNumber()).isZero();
        assertThat(pageable.getPageSize()).isEqualTo(20);
        assertThat(pageable.getSort().getOrderFor("name"))
            .isNotNull()
            .satisfies(order -> assertThat(order.getDirection()).isEqualTo(Sort.Direction.ASC));
    }
    
    @Test
    void listPassesThroughStatusFilterQueryAndPaging() throws Exception {
        when(clientService.list(any(), any(), any()))
            .thenReturn(new PageResponse<>(List.of(), 1, 5, 0L, 0, false));
        
        mockMvc.perform(get("/api/v1/clients")
                            .param("status", "ARCHIVED")
                            .param("q", "acme")
                            .param("page", "1")
                            .param("size", "5")
                            .param("sort", "createdAt,desc")
                            .with(signedIn()))
            .andExpect(status().isOk());
        
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(clientService).list(eq(ClientStatus.ARCHIVED), eq("acme"), captor.capture());
        assertThat(captor.getValue().getPageNumber()).isEqualTo(1);
        assertThat(captor.getValue().getPageSize()).isEqualTo(5);
        assertThat(captor.getValue().getSort().getOrderFor("createdAt")).isNotNull();
    }
    
    @Test
    void rejectsAnUnknownStatusValueWith400() throws Exception {
        // §2.4: wrong types are a malformed request, not a server fault.
        mockMvc.perform(get("/api/v1/clients").param("status", "BOGUS").with(signedIn()))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.type").value("/problems/bad-request"));
    }
    
    @Test
    void getReturnsTheClientResponseShape() throws Exception {
        when(clientService.get(CLIENT_ID)).thenReturn(sampleClient());
        
        mockMvc.perform(get("/api/v1/clients/{id}", CLIENT_ID).with(signedIn()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(CLIENT_ID.toString()))
            .andExpect(jsonPath("$.contactDetails").value("acme@example.com"))
            .andExpect(jsonPath("$.notes").value("Notes"))
            .andExpect(jsonPath("$.archivedAt").doesNotExist())
            .andExpect(jsonPath("$.createdAt").exists())
            .andExpect(jsonPath("$.updatedAt").exists());
    }
    
    @Test
    void getSurfacesAnUnownedOrUnknownClientAs404() throws Exception {
        when(clientService.get(CLIENT_ID)).thenThrow(new ResourceNotFoundException());
        
        mockMvc.perform(get("/api/v1/clients/{id}", CLIENT_ID).with(signedIn()))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.type").value("/problems/resource-not-found"));
    }
    
    @Test
    void rejectsAMalformedClientIdWith400() throws Exception {
        // API_SPEC §2.9: "Malformed UUIDs yield 400".
        mockMvc.perform(get("/api/v1/clients/{id}", "not-a-uuid").with(signedIn()))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.type").value("/problems/bad-request"))
            .andExpect(jsonPath("$.status").value(400));
    }
    
    @Test
    void createReturns201() throws Exception {
        when(clientService.create(any())).thenReturn(sampleClient());
        
        mockMvc.perform(post("/api/v1/clients")
                            .with(signedIn())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"Acme Corp\",\"contactDetails\":\"acme@example.com\",\"notes\":\"Notes\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name").value("Acme Corp"));
        
        verify(clientService).create(eq(new CreateClientRequest("Acme Corp", "acme@example.com", "Notes")));
    }
    
    @Test
    void createRejectsAMissingNameWith422() throws Exception {
        mockMvc.perform(post("/api/v1/clients")
                            .with(signedIn())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"contactDetails\":\"acme@example.com\"}"))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.type").value("/problems/validation-error"))
            .andExpect(jsonPath("$.validationErrors[0].field").value("name"));
    }
    
    @Test
    void createSurfacesServiceValidationAs422WithFieldErrors() throws Exception {
        when(clientService.create(any()))
            .thenThrow(new ValidationFailedException(Map.of("name", "Must be at most 200 characters.")));
        
        mockMvc.perform(post("/api/v1/clients")
                            .with(signedIn())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"way too long\"}"))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.status").value(422))
            .andExpect(jsonPath("$.validationErrors[0].field").value("name"))
            .andExpect(jsonPath("$.validationErrors[0].message").value("Must be at most 200 characters."))
            .andExpect(jsonPath("$.traceId").exists());
    }
    
    @Test
    void updateReturns200AndAcceptsAPartialBody() throws Exception {
        when(clientService.update(eq(CLIENT_ID), any())).thenReturn(sampleClient());
        
        mockMvc.perform(patch("/api/v1/clients/{id}", CLIENT_ID)
                            .with(signedIn())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"notes\":\"Updated notes\"}"))
            .andExpect(status().isOk());
        
        verify(clientService).update(eq(CLIENT_ID), eq(new UpdateClientRequest(null, null, "Updated notes")));
    }
    
    @Test
    void archiveReturns204() throws Exception {
        mockMvc.perform(delete("/api/v1/clients/{id}", CLIENT_ID).with(signedIn()))
            .andExpect(status().isNoContent());
        
        verify(clientService).archive(CLIENT_ID);
    }
    
    @Test
    void archiveSurfacesAnUnownedClientAs404() throws Exception {
        doThrow(new ResourceNotFoundException()).when(clientService).archive(CLIENT_ID);
        
        mockMvc.perform(delete("/api/v1/clients/{id}", CLIENT_ID).with(signedIn()))
            .andExpect(status().isNotFound());
    }
    
    @Test
    void everyEndpointRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/clients")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/clients/{id}", CLIENT_ID)).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/clients")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"Acme\"}"))
            .andExpect(status().isUnauthorized());
        mockMvc.perform(patch("/api/v1/clients/{id}", CLIENT_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"Acme\"}"))
            .andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/v1/clients/{id}", CLIENT_ID)).andExpect(status().isUnauthorized());
    }
}
