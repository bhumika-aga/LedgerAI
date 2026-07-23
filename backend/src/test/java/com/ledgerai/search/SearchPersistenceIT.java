package com.ledgerai.search;

import com.ledgerai.auth.UserAccountRepository;
import com.ledgerai.auth.domain.UserAccount;
import com.ledgerai.clients.ClientRepository;
import com.ledgerai.clients.domain.Client;
import com.ledgerai.documents.DocumentContentRepository;
import com.ledgerai.documents.DocumentRepository;
import com.ledgerai.documents.domain.Document;
import com.ledgerai.documents.domain.DocumentContent;
import com.ledgerai.documents.domain.ExtractionMethod;
import com.ledgerai.documents.domain.ExtractionQuality;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Persistence tests for Global Search over the real PostgreSQL full-text index (API_SPEC §14; DATABASE §9).
 * Exercises the native query end to end — {@code websearch_to_tsquery}/{@code to_tsvector} matching against
 * the existing {@code gin_document_content_extracted_text} index (V5), {@code ts_rank} ordering, owner
 * scoping, and the {@code DELETED} exclusion (BR-013). Skipped where no Docker runtime is available.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class SearchPersistenceIT {
    
    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");
    
    @Autowired
    private DocumentSearchRepository searchRepository;
    @Autowired
    private DocumentRepository documentRepository;
    @Autowired
    private DocumentContentRepository contentRepository;
    @Autowired
    private ClientRepository clientRepository;
    @Autowired
    private UserAccountRepository userRepository;
    
    private UUID newUser() {
        return userRepository.saveAndFlush(
            UserAccount.create(UUID.randomUUID() + "@example.com", "hashed", "Owner")).getId();
    }
    
    private UUID readyDocumentWithText(UUID userId, String filename, String text) {
        UUID clientId = clientRepository.saveAndFlush(Client.create(userId, "Acme Corp", null, null)).getId();
        Document document = Document.create(clientId, filename, "application/pdf", 1234L, "ref-1");
        document.markProcessing();
        document.markReady(ExtractionMethod.NATIVE);
        documentRepository.saveAndFlush(document);
        contentRepository.saveAndFlush(DocumentContent.create(document.getId(), text, ExtractionQuality.HIGH));
        return document.getId();
    }
    
    @Test
    void findsAMatchingDocumentByKeyword() {
        UUID userId = newUser();
        UUID documentId = readyDocumentWithText(userId, "statement.pdf", "Balance sheet total 987654");
        
        Page<SearchResultProjection> results = searchRepository.search(userId, "balance", PageRequest.of(0, 20));
        
        assertThat(results.getContent()).singleElement().satisfies(hit -> {
            assertThat(hit.getDocumentId()).isEqualTo(documentId);
            assertThat(hit.getTitle()).isEqualTo("statement.pdf");
            assertThat(hit.getBodyExcerpt()).contains("Balance sheet total 987654");
            assertThat(hit.getUpdatedAtEpochMs()).isPositive();
        });
    }
    
    @Test
    void returnsEmptyForANonMatchingKeyword() {
        UUID userId = newUser();
        readyDocumentWithText(userId, "statement.pdf", "Balance sheet total 987654");
        
        assertThat(searchRepository.search(userId, "helicopter", PageRequest.of(0, 20)).getContent()).isEmpty();
    }
    
    @Test
    void excludesSoftDeletedDocuments() {
        UUID userId = newUser();
        UUID documentId = readyDocumentWithText(userId, "statement.pdf", "Balance sheet total 987654");
        Document document = documentRepository.findById(documentId).orElseThrow();
        document.markDeleted();
        documentRepository.saveAndFlush(document);
        
        // BR-013: a soft-deleted document is excluded from search.
        assertThat(searchRepository.search(userId, "balance", PageRequest.of(0, 20)).getContent()).isEmpty();
    }
    
    @Test
    void isOwnerScoped() {
        UUID alice = newUser();
        readyDocumentWithText(alice, "alice.pdf", "Balance sheet total 987654");
        UUID bob = newUser();
        
        // Bob searches the same keyword and sees none of Alice's documents (BR-004/006).
        assertThat(searchRepository.search(bob, "balance", PageRequest.of(0, 20)).getContent()).isEmpty();
    }
    
    @Test
    void returnsAllMatchesAcrossTheOwnersDocuments() {
        UUID userId = newUser();
        readyDocumentWithText(userId, "a.pdf", "Quarterly revenue report");
        readyDocumentWithText(userId, "b.pdf", "Annual revenue summary");
        
        assertThat(searchRepository.search(userId, "revenue", PageRequest.of(0, 20)).getTotalElements())
            .isEqualTo(2);
    }
}
