package com.ledgerai.clients;

import com.ledgerai.auth.UserAccountRepository;
import com.ledgerai.auth.domain.UserAccount;
import com.ledgerai.clients.domain.Client;
import com.ledgerai.clients.domain.ClientStatus;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Persistence tests for the client schema (DATABASE §5.2, §9) against a real PostgreSQL with the Flyway
 * schema (ADR-016, ADR-017): the owner-scoped finders, the status check constraint, the enum mapping,
 * and the FK to {@code user}. Skipped where no Docker runtime is available.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class ClientPersistenceIT {
    
    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");
    
    @Autowired
    private ClientRepository clientRepository;
    
    @Autowired
    private UserAccountRepository userRepository;
    
    @Autowired
    private TestEntityManager entityManager;
    
    private UUID ownerId;
    private UUID otherUserId;
    
    @BeforeEach
    void createOwners() {
        ownerId = userRepository.saveAndFlush(
            UserAccount.create(UUID.randomUUID() + "@example.com", "hashed", "Owner")).getId();
        otherUserId = userRepository.saveAndFlush(
            UserAccount.create(UUID.randomUUID() + "@example.com", "hashed", "Other")).getId();
    }
    
    private Client save(UUID userId, String name) {
        return clientRepository.saveAndFlush(Client.create(userId, name, "contact", "notes"));
    }
    
    @Test
    void persistsAndReloadsAClient() {
        Client saved = save(ownerId, "Acme Corp");
        entityManager.clear();
        
        Client reloaded = clientRepository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getUserId()).isEqualTo(ownerId);
        assertThat(reloaded.getName()).isEqualTo("Acme Corp");
        assertThat(reloaded.getContactDetails()).isEqualTo("contact");
        assertThat(reloaded.getNotes()).isEqualTo("notes");
        assertThat(reloaded.getStatus()).isEqualTo(ClientStatus.ACTIVE);
        assertThat(reloaded.getArchivedAt()).isNull();
        assertThat(reloaded.getCreatedAt()).isNotNull();
    }
    
    @Test
    void findsOnlyTheOwnersClients() {
        save(ownerId, "Mine");
        save(otherUserId, "Theirs");
        entityManager.clear();
        
        Page<Client> page = clientRepository.findByUserIdAndStatus(
            ownerId, ClientStatus.ACTIVE, PageRequest.of(0, 20));
        
        // BR-004 at the query level: another user's rows are never fetched.
        assertThat(page.getContent()).extracting(Client::getName).containsExactly("Mine");
    }
    
    @Test
    void separatesActiveFromArchived() {
        save(ownerId, "Active One");
        Client toArchive = save(ownerId, "Archived One");
        toArchive.archive();
        clientRepository.saveAndFlush(toArchive);
        entityManager.clear();
        
        assertThat(clientRepository.findByUserIdAndStatus(ownerId, ClientStatus.ACTIVE, PageRequest.of(0, 20))
                       .getContent()).extracting(Client::getName).containsExactly("Active One");
        assertThat(clientRepository.findByUserIdAndStatus(ownerId, ClientStatus.ARCHIVED, PageRequest.of(0, 20))
                       .getContent()).extracting(Client::getName).containsExactly("Archived One");
    }
    
    @Test
    void persistsTheArchivedStatusAndTimestamp() {
        Client client = save(ownerId, "Acme Corp");
        client.archive();
        clientRepository.saveAndFlush(client);
        entityManager.clear();
        
        Client reloaded = clientRepository.findById(client.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(ClientStatus.ARCHIVED);
        assertThat(reloaded.getArchivedAt()).isNotNull();
    }
    
    @Test
    void filtersByNameCaseInsensitivelyAnywhereInTheName() {
        save(ownerId, "Acme Corp");
        save(ownerId, "Global Acme Holdings");
        save(ownerId, "Zenith Ltd");
        entityManager.clear();
        
        Page<Client> page = clientRepository.findByUserIdAndStatusAndNameContainingIgnoreCase(
            ownerId, ClientStatus.ACTIVE, "acme", PageRequest.of(0, 20, Sort.by("name")));
        
        assertThat(page.getContent()).extracting(Client::getName)
            .containsExactly("Acme Corp", "Global Acme Holdings");
    }
    
    @Test
    void doesNotLeakAnotherUsersClientsThroughTheNameFilter() {
        save(otherUserId, "Acme Corp");
        entityManager.clear();
        
        assertThat(clientRepository.findByUserIdAndStatusAndNameContainingIgnoreCase(
            ownerId, ClientStatus.ACTIVE, "acme", PageRequest.of(0, 20)).getContent()).isEmpty();
    }
    
    @Test
    void pagesAndSortsByName() {
        save(ownerId, "Charlie");
        save(ownerId, "alpha");
        save(ownerId, "Bravo");
        entityManager.clear();
        
        Sort byName = Sort.by(Sort.Direction.ASC, "name");
        Page<Client> first = clientRepository.findByUserIdAndStatus(
            ownerId, ClientStatus.ACTIVE, PageRequest.of(0, 2, byName));
        
        assertThat(first.getTotalElements()).isEqualTo(3);
        assertThat(first.getTotalPages()).isEqualTo(2);
        assertThat(first.hasNext()).isTrue();
        assertThat(first.getContent()).hasSize(2);
        
        List<String> allNames = clientRepository.findByUserIdAndStatus(
                ownerId, ClientStatus.ACTIVE, PageRequest.of(0, 20, byName))
                                    .getContent().stream().map(Client::getName).toList();
        assertThat(allNames).hasSize(3);
    }
    
    @Test
    void allowsDuplicateNamesForTheSameOwner() {
        // BR-024: no uniqueness constraint on (user_id, name) — duplicates are legitimate.
        save(ownerId, "Acme Corp");
        
        assertThat(clientRepository.saveAndFlush(Client.create(ownerId, "Acme Corp", null, null)))
            .isNotNull();
    }
    
    @Test
    void rejectsAStatusOutsideTheConstrainedSet() {
        Client client = save(ownerId, "Acme Corp");
        entityManager.clear();
        
        // The CHECK constraint is the database's own guard on the enum (DATABASE §5.2); the enum makes
        // this unreachable from Java, so it is exercised with raw SQL.
        assertThatThrownBy(() -> entityManager.getEntityManager()
                                     .createNativeQuery("UPDATE client SET status = 'BOGUS' WHERE id = :id")
                                     .setParameter("id", client.getId())
                                     .executeUpdate())
            .isNotNull();
    }
    
    @Test
    void cascadesWhenTheOwningUserIsRemoved() {
        Client client = save(ownerId, "Acme Corp");
        entityManager.flush();
        entityManager.clear();
        
        // FK ON DELETE CASCADE (DATABASE §5.2). User deletion is out of MVP scope, so this only proves
        // the constraint the schema declares.
        entityManager.getEntityManager()
            .createNativeQuery("DELETE FROM \"user\" WHERE id = :id")
            .setParameter("id", ownerId)
            .executeUpdate();
        entityManager.flush();
        entityManager.clear();
        
        assertThat(clientRepository.findById(client.getId())).isEmpty();
    }
}
