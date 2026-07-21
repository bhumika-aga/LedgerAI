package com.ledgerai.activity;

import com.ledgerai.activity.domain.Activity;
import com.ledgerai.activity.domain.ActivityType;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Persistence tests for the {@code activity} schema (DATABASE §5.8, §9) against a real PostgreSQL with the
 * Flyway schema (ADR-016, V7): owner-scoped and per-client reads, the enum + jsonb mapping, the immutable
 * shape (no updated_at), the {@code SET NULL} FKs that keep history when a client/document is removed, and
 * the {@code CASCADE} FK on the owning user. Skipped where no Docker runtime is available.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class ActivityPersistenceIT {
    
    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");
    
    @Autowired
    private ActivityRepository activityRepository;
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
    private UUID documentId;
    
    @BeforeEach
    void seed() {
        userId = userRepository.saveAndFlush(
            UserAccount.create(UUID.randomUUID() + "@example.com", "hashed", "Owner")).getId();
        clientId = clientRepository.saveAndFlush(Client.create(userId, "Acme Corp", null, null)).getId();
        documentId = documentRepository.saveAndFlush(
            Document.create(clientId, "statement.pdf", "application/pdf", 1234L, "ref-1")).getId();
    }
    
    @Test
    void persistsAndReloadsWithEnumJsonbAndCreatedAt() {
        Activity saved = activityRepository.saveAndFlush(Activity.record(
            ActivityType.DOCUMENT_UPLOADED, userId, clientId, documentId,
            "Uploaded document \"statement.pdf\"", Map.of("filename", "statement.pdf")));
        entityManager.clear();
        
        Activity reloaded = activityRepository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getActionType()).isEqualTo(ActivityType.DOCUMENT_UPLOADED);
        assertThat(reloaded.getSummary()).contains("statement.pdf");
        assertThat(reloaded.getMetadata()).containsEntry("filename", "statement.pdf");
        assertThat(reloaded.getCreatedAt()).isNotNull();
    }
    
    @Test
    void readsAreOwnerScopedAndSupportThePerClientView() {
        activityRepository.saveAndFlush(Activity.record(
            ActivityType.CLIENT_CREATED, userId, clientId, null, "Created client \"Acme\"", null));
        activityRepository.saveAndFlush(Activity.record(
            ActivityType.SUMMARY_GENERATED, userId, null, documentId, "Generated an AI summary", null));
        UUID otherUser = userRepository.saveAndFlush(
            UserAccount.create(UUID.randomUUID() + "@example.com", "h", "Other")).getId();
        activityRepository.saveAndFlush(Activity.record(
            ActivityType.CLIENT_CREATED, otherUser, null, null, "Other user's activity", null));
        
        var pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
        // Account view: only the owner's two rows, never the other user's (BR-006).
        assertThat(activityRepository.findByUserId(userId, pageable).getTotalElements()).isEqualTo(2);
        // Per-client view: only the client-scoped row (the summary has null client_id).
        assertThat(activityRepository.findByUserIdAndClientId(userId, clientId, pageable).getContent())
            .singleElement()
            .satisfies(a -> assertThat(a.getActionType()).isEqualTo(ActivityType.CLIENT_CREATED));
    }
    
    @Test
    void keepsHistoryWhenTheReferencedClientIsRemoved() {
        Activity activity = activityRepository.saveAndFlush(Activity.record(
            ActivityType.DOCUMENT_UPLOADED, userId, clientId, documentId, "Uploaded", null));
        entityManager.flush();
        entityManager.clear();
        
        // FK activity.client_id → client ON DELETE SET NULL (DATABASE §5.8): the row survives, unlinked.
        entityManager.getEntityManager()
            .createNativeQuery("DELETE FROM document WHERE id = :d").setParameter("d", documentId).executeUpdate();
        entityManager.getEntityManager()
            .createNativeQuery("DELETE FROM client WHERE id = :c").setParameter("c", clientId).executeUpdate();
        entityManager.flush();
        entityManager.clear();
        
        Activity reloaded = activityRepository.findById(activity.getId()).orElseThrow();
        assertThat(reloaded.getClientId()).isNull();
        assertThat(reloaded.getDocumentId()).isNull();
    }
    
    @Test
    void cascadesWhenTheOwningUserIsRemoved() {
        Activity activity = activityRepository.saveAndFlush(Activity.record(
            ActivityType.CLIENT_CREATED, userId, clientId, null, "Created", null));
        entityManager.flush();
        entityManager.clear();
        
        // FK activity.user_id → user ON DELETE CASCADE (DATABASE §5.8). Remove dependents first.
        entityManager.getEntityManager()
            .createNativeQuery("DELETE FROM document WHERE id = :d").setParameter("d", documentId).executeUpdate();
        entityManager.getEntityManager()
            .createNativeQuery("DELETE FROM client WHERE id = :c").setParameter("c", clientId).executeUpdate();
        entityManager.getEntityManager()
            .createNativeQuery("DELETE FROM \"user\" WHERE id = :u").setParameter("u", userId).executeUpdate();
        entityManager.flush();
        entityManager.clear();
        
        assertThat(activityRepository.findById(activity.getId())).isEmpty();
    }
}
