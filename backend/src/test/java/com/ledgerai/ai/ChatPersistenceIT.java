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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Persistence tests for the AI Chat access paths on the shared {@code ai_request}/{@code ai_output} schema
 * (DATABASE §5.5–5.6; §3.1 chat note; no new migration — {@code type} already permits {@code CHAT}) against
 * a real PostgreSQL with the Flyway schema (ADR-016, V6): the {@code CHAT} type round-trips with its
 * question retained as {@code prompt}, a document's thread is read chronologically and filtered by type,
 * and answers batch-load by request id. Skipped where no Docker runtime is available.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class ChatPersistenceIT {
    
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
    void persistsAChatRequestRetainingTheQuestionAsPrompt() {
        AiRequest request = AiRequest.createChat(userId, documentId, "What is the total?");
        request.markInProgress();
        request.markCompleted();
        requestRepository.saveAndFlush(request);
        outputRepository.saveAndFlush(AiOutput.create(request.getId(), "The total is 987654."));
        entityManager.clear();
        
        AiRequest reloaded = requestRepository.findById(request.getId()).orElseThrow();
        assertThat(reloaded.getType()).isEqualTo(AiRequestType.CHAT);
        assertThat(reloaded.getPrompt()).isEqualTo("What is the total?");
        assertThat(outputRepository.findByAiRequestId(request.getId()).orElseThrow().getContent())
            .isEqualTo("The total is 987654.");
    }
    
    @Test
    void readsTheDocumentThreadChronologicallyAndOnlyChatType() {
        // Two chat exchanges plus a summary request on the same document — only CHAT rows, oldest first.
        AiRequest first = requestRepository.saveAndFlush(AiRequest.createChat(userId, documentId, "Q1"));
        AiRequest second = requestRepository.saveAndFlush(AiRequest.createChat(userId, documentId, "Q2"));
        requestRepository.saveAndFlush(AiRequest.createSummary(userId, documentId));
        entityManager.clear();
        
        Page<AiRequest> thread = requestRepository.findByDocumentIdAndType(documentId, AiRequestType.CHAT,
            PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "createdAt")));
        
        assertThat(thread.getTotalElements()).isEqualTo(2);
        assertThat(thread.getContent()).extracting(AiRequest::getId)
            .containsExactly(first.getId(), second.getId());
        assertThat(thread.getContent()).allMatch(r -> r.getType() == AiRequestType.CHAT);
    }
    
    @Test
    void batchLoadsAnswersByRequestId() {
        AiRequest a = requestRepository.saveAndFlush(AiRequest.createChat(userId, documentId, "Qa"));
        AiRequest b = requestRepository.saveAndFlush(AiRequest.createChat(userId, documentId, "Qb"));
        outputRepository.saveAndFlush(AiOutput.create(a.getId(), "Answer A"));
        outputRepository.saveAndFlush(AiOutput.create(b.getId(), "Answer B"));
        entityManager.clear();
        
        List<AiOutput> outputs = outputRepository.findByAiRequestIdIn(List.of(a.getId(), b.getId()));
        
        assertThat(outputs).extracting(AiOutput::getContent)
            .containsExactlyInAnyOrder("Answer A", "Answer B");
    }
}
