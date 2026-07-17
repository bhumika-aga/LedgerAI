package com.ledgerai.documents;

import com.ledgerai.auth.UserAccountRepository;
import com.ledgerai.auth.domain.UserAccount;
import com.ledgerai.clients.ClientRepository;
import com.ledgerai.clients.domain.Client;
import com.ledgerai.documents.domain.Document;
import com.ledgerai.documents.domain.DocumentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Persistence tests for the document schema (DATABASE §5.3, §9) against a real PostgreSQL with the
 * Flyway schema (ADR-016, ADR-017): the deleted-excluding finders, the status check constraint, the
 * enum mapping, and the FK cascade from client. Skipped where no Docker runtime is available.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class DocumentPersistenceIT {
    
    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");
    
    @Autowired
    private DocumentRepository documentRepository;
    
    @Autowired
    private ClientRepository clientRepository;
    
    @Autowired
    private UserAccountRepository userRepository;
    
    @Autowired
    private TestEntityManager entityManager;
    
    private UUID clientId;
    
    @BeforeEach
    void createOwningClient() {
        UUID userId = userRepository.saveAndFlush(
            UserAccount.create(UUID.randomUUID() + "@example.com", "hashed", "Owner")).getId();
        clientId = clientRepository.saveAndFlush(Client.create(userId, "Acme Corp", null, null)).getId();
    }
    
    private Document save(String filename) {
        return documentRepository.saveAndFlush(
            Document.create(clientId, filename, "application/pdf", 1234L, "ref-" + filename));
    }
    
    @Test
    void persistsAndReloadsADocument() {
        Document saved = save("statement.pdf");
        entityManager.clear();
        
        Document reloaded = documentRepository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getClientId()).isEqualTo(clientId);
        assertThat(reloaded.getOriginalFilename()).isEqualTo("statement.pdf");
        assertThat(reloaded.getMimeType()).isEqualTo("application/pdf");
        assertThat(reloaded.getSizeBytes()).isEqualTo(1234L);
        assertThat(reloaded.getStorageReference()).isEqualTo("ref-statement.pdf");
        assertThat(reloaded.getStatus()).isEqualTo(DocumentStatus.UPLOADED);
        assertThat(reloaded.getExtractionMethod()).isNull();
        assertThat(reloaded.getDeletedAt()).isNull();
    }
    
    @Test
    void listExcludesSoftDeletedDocuments() {
        save("active.pdf");
        Document toDelete = save("gone.pdf");
        toDelete.markDeleted();
        documentRepository.saveAndFlush(toDelete);
        entityManager.clear();
        
        Page<Document> page = documentRepository.findByClientIdAndStatusNot(
            clientId, DocumentStatus.DELETED, PageRequest.of(0, 20));
        
        assertThat(page.getContent()).extracting(Document::getOriginalFilename).containsExactly("active.pdf");
    }
    
    @Test
    void findByIdExcludingDeletedHidesADeletedDocument() {
        Document document = save("gone.pdf");
        document.markDeleted();
        documentRepository.saveAndFlush(document);
        entityManager.clear();
        
        assertThat(documentRepository.findByIdAndStatusNot(document.getId(), DocumentStatus.DELETED)).isEmpty();
        // The row is retained (soft delete), just excluded from retrieval.
        assertThat(documentRepository.findById(document.getId())).isPresent();
    }
    
    @Test
    void filtersByStatus() {
        save("one.pdf");
        entityManager.clear();
        
        assertThat(documentRepository.findByClientIdAndStatus(clientId, DocumentStatus.UPLOADED, PageRequest.of(0, 20))
                       .getContent()).hasSize(1);
        assertThat(documentRepository.findByClientIdAndStatus(clientId, DocumentStatus.READY, PageRequest.of(0, 20))
                       .getContent()).isEmpty();
    }
    
    @Test
    void rejectsAStatusOutsideTheConstrainedSet() {
        Document document = save("statement.pdf");
        entityManager.clear();
        
        // The CHECK constraint guards the enum at the database (DATABASE §5.3); unreachable from Java.
        assertThatThrownBy(() -> entityManager.getEntityManager()
                                     .createNativeQuery("UPDATE document SET status = 'BOGUS' WHERE id = :id")
                                     .setParameter("id", document.getId())
                                     .executeUpdate())
            .isNotNull();
    }
    
    @Test
    void cascadesWhenTheOwningClientIsRemoved() {
        Document document = save("statement.pdf");
        entityManager.flush();
        entityManager.clear();
        
        // FK ON DELETE CASCADE (DATABASE §5.3).
        entityManager.getEntityManager()
            .createNativeQuery("DELETE FROM client WHERE id = :id")
            .setParameter("id", clientId)
            .executeUpdate();
        entityManager.flush();
        entityManager.clear();
        
        assertThat(documentRepository.findById(document.getId())).isEmpty();
    }
}
