package com.ledgerai.documents;

import com.ledgerai.auth.UserAccountRepository;
import com.ledgerai.auth.domain.UserAccount;
import com.ledgerai.clients.ClientRepository;
import com.ledgerai.clients.domain.Client;
import com.ledgerai.documents.domain.Document;
import com.ledgerai.documents.domain.DocumentContent;
import com.ledgerai.documents.domain.ExtractionQuality;
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
 * Persistence tests for the {@code document_content} schema (DATABASE §5.4, §9) against a real
 * PostgreSQL with the Flyway schema (ADR-016, ADR-017): the 1:1 unique constraint, the FK cascade from
 * document, the enum mapping, and char_count. Skipped where no Docker runtime is available.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class DocumentContentPersistenceIT {
    
    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");
    
    @Autowired
    private DocumentContentRepository contentRepository;
    
    @Autowired
    private DocumentRepository documentRepository;
    
    @Autowired
    private ClientRepository clientRepository;
    
    @Autowired
    private UserAccountRepository userRepository;
    
    @Autowired
    private TestEntityManager entityManager;
    
    private UUID documentId;
    
    @BeforeEach
    void createOwningDocument() {
        UUID userId = userRepository.saveAndFlush(
            UserAccount.create(UUID.randomUUID() + "@example.com", "hashed", "Owner")).getId();
        UUID clientId = clientRepository.saveAndFlush(Client.create(userId, "Acme Corp", null, null)).getId();
        Document document = documentRepository.saveAndFlush(
            Document.create(clientId, "statement.pdf", "application/pdf", 1234L, "ref-1"));
        documentId = document.getId();
    }
    
    @Test
    void persistsAndReloadsExtractedContentWithCharCount() {
        contentRepository.saveAndFlush(
            DocumentContent.create(documentId, "Invoice total 1234.56", ExtractionQuality.HIGH));
        entityManager.clear();
        
        DocumentContent reloaded = contentRepository.findByDocumentId(documentId).orElseThrow();
        assertThat(reloaded.getExtractedText()).isEqualTo("Invoice total 1234.56");
        assertThat(reloaded.getExtractionQuality()).isEqualTo(ExtractionQuality.HIGH);
        assertThat(reloaded.getCharCount()).isEqualTo("Invoice total 1234.56".length());
    }
    
    @Test
    void enforcesOneContentRowPerDocument() {
        contentRepository.saveAndFlush(DocumentContent.create(documentId, "first", ExtractionQuality.LOW));
        
        // UNIQUE(document_id) — the 1:1 relationship (DATABASE §5.4).
        assertThatThrownBy(() -> contentRepository.saveAndFlush(
            DocumentContent.create(documentId, "second", ExtractionQuality.LOW))).isNotNull();
    }
    
    @Test
    void cascadesWhenTheOwningDocumentIsRemoved() {
        contentRepository.saveAndFlush(DocumentContent.create(documentId, "text", ExtractionQuality.HIGH));
        entityManager.flush();
        entityManager.clear();
        
        // FK ON DELETE CASCADE (DATABASE §5.4).
        entityManager.getEntityManager()
            .createNativeQuery("DELETE FROM document WHERE id = :id")
            .setParameter("id", documentId)
            .executeUpdate();
        entityManager.flush();
        entityManager.clear();
        
        assertThat(contentRepository.findByDocumentId(documentId)).isEmpty();
    }
}
