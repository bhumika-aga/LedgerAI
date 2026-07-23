package com.ledgerai.ai;

import com.ledgerai.ai.domain.AiOutput;
import com.ledgerai.ai.domain.AiRequest;
import com.ledgerai.ai.domain.AiRequestType;
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

/**
 * Persistence tests for AI Email on the shared {@code ai_request}/{@code ai_output} schema (DATABASE
 * §5.5–5.6; no new migration — {@code type} already permits {@code EMAIL} and {@code document_id} is
 * nullable) against a real PostgreSQL with the Flyway schema (ADR-016, V6): an {@code EMAIL} request
 * round-trips with its instruction retained as {@code prompt}, and — unlike summary/chat — persists with a
 * <strong>null</strong> {@code document_id} when no document context was given. Skipped where no Docker
 * runtime is available.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class EmailPersistenceIT {
    
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
    private UUID clientId;
    
    @BeforeEach
    void createOwner() {
        userId = userRepository.saveAndFlush(
            UserAccount.create(UUID.randomUUID() + "@example.com", "hashed", "Owner")).getId();
        clientId = clientRepository.saveAndFlush(Client.create(userId, "Acme Corp", null, null)).getId();
    }
    
    @Test
    void persistsAnEmailRequestWithNoDocumentContext() {
        // Email context is optional (API_SPEC §12.1): document_id is null. The instruction is the prompt.
        AiRequest request = AiRequest.createEmail(userId, null, "Write a follow-up email.");
        request.markInProgress();
        request.markCompleted();
        requestRepository.saveAndFlush(request);
        outputRepository.saveAndFlush(AiOutput.create(request.getId(), "Dear client, ..."));
        entityManager.clear();
        
        AiRequest reloaded = requestRepository.findById(request.getId()).orElseThrow();
        assertThat(reloaded.getType()).isEqualTo(AiRequestType.EMAIL);
        assertThat(reloaded.getDocumentId()).isNull();
        assertThat(reloaded.getPrompt()).isEqualTo("Write a follow-up email.");
        assertThat(outputRepository.findByAiRequestId(request.getId()).orElseThrow().getContent())
            .isEqualTo("Dear client, ...");
    }
    
    @Test
    void persistsAnEmailRequestWithDocumentContext() {
        UUID documentId = documentRepository.saveAndFlush(
            Document.create(clientId, "statement.pdf", "application/pdf", 1234L, "ref-1")).getId();
        
        AiRequest request = requestRepository.saveAndFlush(
            AiRequest.createEmail(userId, documentId, "Summarize this for the client."));
        entityManager.clear();
        
        AiRequest reloaded = requestRepository.findById(request.getId()).orElseThrow();
        assertThat(reloaded.getType()).isEqualTo(AiRequestType.EMAIL);
        assertThat(reloaded.getDocumentId()).isEqualTo(documentId);
    }
}
