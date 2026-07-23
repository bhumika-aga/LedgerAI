package com.ledgerai.reports;

import com.ledgerai.auth.UserAccountRepository;
import com.ledgerai.auth.domain.UserAccount;
import com.ledgerai.clients.ClientRepository;
import com.ledgerai.clients.domain.Client;
import com.ledgerai.documents.DocumentRepository;
import com.ledgerai.documents.domain.Document;
import com.ledgerai.reports.domain.Report;
import com.ledgerai.reports.domain.ReportStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Persistence tests for the {@code report} schema (DATABASE §5.7, §9) against a real PostgreSQL with the
 * Flyway schema (ADR-016, V8): the status enum mapping, the owner-scoped filtered list, and the FK cascade
 * from document/user. Skipped where no Docker runtime is available.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class ReportPersistenceIT {
    
    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");
    
    @Autowired
    private ReportRepository reportRepository;
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
    void seed() {
        userId = userRepository.saveAndFlush(
            UserAccount.create(UUID.randomUUID() + "@example.com", "hashed", "Owner")).getId();
        UUID clientId = clientRepository.saveAndFlush(Client.create(userId, "Acme Corp", null, null)).getId();
        documentId = documentRepository.saveAndFlush(
            Document.create(clientId, "statement.pdf", "application/pdf", 1234L, "ref-1")).getId();
    }
    
    @Test
    void persistsAndReloadsADraftReport() {
        Report saved = reportRepository.saveAndFlush(
            Report.createDraft(userId, documentId, "Q4 Review", "The report body."));
        entityManager.clear();
        
        Report reloaded = reportRepository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(ReportStatus.DRAFT);
        assertThat(reloaded.getTitle()).isEqualTo("Q4 Review");
        assertThat(reloaded.getContent()).isEqualTo("The report body.");
        assertThat(reloaded.getDocumentId()).isEqualTo(documentId);
    }
    
    @Test
    void listIsOwnerScopedAndFilterable() {
        Report draft = reportRepository.saveAndFlush(Report.createDraft(userId, documentId, "Draft", "d"));
        Report saved = Report.createDraft(userId, documentId, "Saved", "s");
        saved.applyUpdate(null, null, ReportStatus.SAVED);
        reportRepository.saveAndFlush(saved);
        UUID otherUser = userRepository.saveAndFlush(
            UserAccount.create(UUID.randomUUID() + "@example.com", "h", "Other")).getId();
        reportRepository.saveAndFlush(Report.createDraft(otherUser, documentId, "Other", "o"));
        
        var pageable = PageRequest.of(0, 20);
        // Account view: only the owner's two reports (BR-006).
        assertThat(reportRepository.findOwned(userId, null, null, pageable).getTotalElements()).isEqualTo(2);
        // Filter by status.
        assertThat(reportRepository.findOwned(userId, null, ReportStatus.SAVED, pageable).getContent())
            .singleElement().satisfies(r -> assertThat(r.getId()).isEqualTo(saved.getId()));
        // Filter by document.
        assertThat(reportRepository.findOwned(userId, documentId, null, pageable).getTotalElements()).isEqualTo(2);
        // A different user's filter never sees these.
        assertThat(reportRepository.findOwned(otherUser, null, ReportStatus.DRAFT, pageable).getContent())
            .singleElement().satisfies(r -> assertThat(r.getId()).isNotEqualTo(draft.getId()));
    }
    
    @Test
    void cascadesWhenTheOwningDocumentIsRemoved() {
        Report saved = reportRepository.saveAndFlush(
            Report.createDraft(userId, documentId, "Q4", "body"));
        entityManager.flush();
        entityManager.clear();
        
        // FK report.document_id → document ON DELETE CASCADE (DATABASE §5.7).
        entityManager.getEntityManager()
            .createNativeQuery("DELETE FROM document WHERE id = :id")
            .setParameter("id", documentId)
            .executeUpdate();
        entityManager.flush();
        entityManager.clear();
        
        assertThat(reportRepository.findById(saved.getId())).isEmpty();
    }
}
