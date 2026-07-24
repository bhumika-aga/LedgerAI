package com.ledgerai.ai;

import com.ledgerai.ai.domain.AiOutput;
import com.ledgerai.ai.domain.AiRequest;
import com.ledgerai.ai.domain.AiRequestStatus;
import com.ledgerai.auth.UserAccountRepository;
import com.ledgerai.auth.domain.UserAccount;
import com.ledgerai.clients.ClientRepository;
import com.ledgerai.clients.domain.Client;
import com.ledgerai.documents.DocumentRepository;
import com.ledgerai.documents.domain.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Persistence tests for the {@code ai_request}/{@code ai_output} schema (DATABASE §5.5–5.6, §9) against a
 * real PostgreSQL with the Flyway schema (ADR-016, V6): the lifecycle status mapping, the 1:1 unique
 * constraint, the FK cascade from request to output, and the cascade from document to request. Skipped
 * where no Docker runtime is available.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class AiSummaryPersistenceIT {
    
    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");
    
    @Autowired
    private AiRequestRepository requestRepository;
    @Autowired
    private AiOutputRepository outputRepository;
    @Autowired
    private DocumentRepository documentRepository;
    @Autowired
    private ClientRepository clientRepository;
    @Autowired
    private UserAccountRepository userRepository;
    @Autowired
    private TestEntityManager entityManager;
    
    private UUID userId;
    private UUID documentId;
    
    @BeforeEach
    void createOwningDocument() {
        userId = userRepository.saveAndFlush(
            UserAccount.create(UUID.randomUUID() + "@example.com", "hashed", "Owner")).getId();
        UUID clientId = clientRepository.saveAndFlush(Client.create(userId, "Acme Corp", null, null)).getId();
        documentId = documentRepository.saveAndFlush(
            Document.create(clientId, "statement.pdf", "application/pdf", 1234L, "ref-1")).getId();
    }
    
    @Test
    void persistsAndReloadsACompletedRequestWithItsEditableOutput() {
        AiRequest request = AiRequest.createSummary(userId, documentId);
        request.markInProgress();
        request.markCompleted();
        requestRepository.saveAndFlush(request);
        outputRepository.saveAndFlush(AiOutput.create(request.getId(), "Grounded summary."));
        entityManager.clear();
        
        AiRequest reloaded = requestRepository.findById(request.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(AiRequestStatus.COMPLETED);
        assertThat(reloaded.getDocumentId()).isEqualTo(documentId);
        
        AiOutput output = outputRepository.findByAiRequestId(request.getId()).orElseThrow();
        assertThat(output.getContent()).isEqualTo("Grounded summary.");
        assertThat(output.isEdited()).isFalse();
    }
    
    @Test
    void enforcesOneOutputPerRequest() {
        AiRequest request = requestRepository.saveAndFlush(AiRequest.createSummary(userId, documentId));
        outputRepository.saveAndFlush(AiOutput.create(request.getId(), "first"));
        
        // UNIQUE(ai_request_id) — the 1:1 relationship (DATABASE §5.6).
        assertThatThrownBy(() -> outputRepository.saveAndFlush(
            AiOutput.create(request.getId(), "second"))).isNotNull();
    }
    
    @Test
    void cascadesOutputWhenTheOwningRequestIsRemoved() {
        AiRequest request = requestRepository.saveAndFlush(AiRequest.createSummary(userId, documentId));
        outputRepository.saveAndFlush(AiOutput.create(request.getId(), "text"));
        entityManager.flush();
        entityManager.clear();
        
        // FK ai_output → ai_request ON DELETE CASCADE (DATABASE §5.6).
        entityManager.getEntityManager()
            .createNativeQuery("DELETE FROM ai_request WHERE id = :id")
            .setParameter("id", request.getId())
            .executeUpdate();
        entityManager.flush();
        entityManager.clear();
        
        assertThat(outputRepository.findByAiRequestId(request.getId())).isEmpty();
    }
    
    @Test
    void cascadesRequestAndOutputWhenTheDocumentIsRemoved() {
        AiRequest request = requestRepository.saveAndFlush(AiRequest.createSummary(userId, documentId));
        outputRepository.saveAndFlush(AiOutput.create(request.getId(), "text"));
        entityManager.flush();
        entityManager.clear();
        
        // FK ai_request → document ON DELETE CASCADE (DATABASE §5.5), which then cascades to ai_output.
        entityManager.getEntityManager()
            .createNativeQuery("DELETE FROM document WHERE id = :id")
            .setParameter("id", documentId)
            .executeUpdate();
        entityManager.flush();
        entityManager.clear();
        
        assertThat(requestRepository.findById(request.getId())).isEmpty();
        assertThat(outputRepository.findByAiRequestId(request.getId())).isEmpty();
    }
}
