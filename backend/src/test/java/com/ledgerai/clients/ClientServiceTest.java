package com.ledgerai.clients;

import com.ledgerai.clients.config.ClientProperties;
import com.ledgerai.clients.domain.Client;
import com.ledgerai.clients.domain.ClientStatus;
import com.ledgerai.clients.dto.ClientResponse;
import com.ledgerai.clients.dto.CreateClientRequest;
import com.ledgerai.clients.dto.UpdateClientRequest;
import com.ledgerai.common.dto.PageResponse;
import com.ledgerai.common.exception.ResourceNotFoundException;
import com.ledgerai.common.security.CurrentUserProvider;
import com.ledgerai.common.security.OwnershipGuard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the client business rules (SRS §4.3: FR-CLNT-001…005; VR-004; BR-002/003/024).
 *
 * <p>The {@link OwnershipGuard} is real, not mocked: it is the control under test as much as the
 * service is, and stubbing it would prove only that the service calls a mock. Its own collaborator
 * ({@link CurrentUserProvider}) is mocked instead, which is what actually varies per test.
 */
@ExtendWith(MockitoExtension.class)
class ClientServiceTest {
    
    private static final int MAX_NAME = 200;
    private static final int MAX_CONTACT = 20;
    private static final int MAX_NOTES = 30;
    
    @Mock
    private ClientRepository clientRepository;
    
    @Mock
    private CurrentUserProvider currentUserProvider;
    
    private ClientService service;
    private UUID userId;
    
    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        service = new ClientService(
            clientRepository,
            currentUserProvider,
            new OwnershipGuard(currentUserProvider),
            new ClientProperties(MAX_NAME, MAX_CONTACT, MAX_NOTES));
    }
    
    private void signedIn() {
        when(currentUserProvider.requireUserId()).thenReturn(userId);
    }
    
    private Client ownedClient() {
        return Client.create(userId, "Acme Corp", "acme@example.com", "Notes");
    }
    
    private void expectSave() {
        when(clientRepository.save(any(Client.class))).thenAnswer(call -> call.getArgument(0));
    }
    
    @Test
    void createsAClientOwnedByTheCaller() {
        signedIn();
        expectSave();
        
        ClientResponse response = service.create(
            new CreateClientRequest("Acme Corp", "acme@example.com", "Notes"));
        
        assertThat(response.name()).isEqualTo("Acme Corp");
        assertThat(response.status()).isEqualTo(ClientStatus.ACTIVE);
        assertThat(response.archivedAt()).isNull();
        verify(clientRepository).save(any(Client.class));
    }
    
    @Test
    void allowsDuplicateNames() {
        // BR-024: real clients can share a name; a duplicate is explicitly not an error.
        signedIn();
        expectSave();
        
        assertThatCode(() -> service.create(new CreateClientRequest("Acme Corp", null, null)))
            .doesNotThrowAnyException();
        // Nothing is looked up to police uniqueness.
        verify(clientRepository, never()).findByUserIdAndStatus(any(), any(), any());
    }
    
    @Test
    void rejectsABlankName() {
        signedIn();
        
        var thrown = catchThrowableOfType(
            () -> service.create(new CreateClientRequest("   ", null, null)),
            com.ledgerai.common.exception.ValidationFailedException.class);
        
        assertThat(thrown.getFieldErrors()).containsOnlyKeys("name");
        verify(clientRepository, never()).save(any());
    }
    
    @Test
    void rejectsAnOverLongName() {
        signedIn();
        
        var thrown = catchThrowableOfType(
            () -> service.create(new CreateClientRequest("x".repeat(MAX_NAME + 1), null, null)),
            com.ledgerai.common.exception.ValidationFailedException.class);
        
        assertThat(thrown.getFieldErrors()).containsOnlyKeys("name");
    }
    
    @Test
    void reportsEveryInvalidFieldAtOnce() {
        signedIn();
        
        var thrown = catchThrowableOfType(
            () -> service.create(new CreateClientRequest(
                "", "c".repeat(MAX_CONTACT + 1), "n".repeat(MAX_NOTES + 1))),
            com.ledgerai.common.exception.ValidationFailedException.class);
        
        assertThat(thrown.getFieldErrors()).containsOnlyKeys("name", "contactDetails", "notes");
    }
    
    @Test
    void listsOnlyTheCallersActiveClientsByDefault() {
        signedIn();
        Pageable pageable = PageRequest.of(0, 20, Sort.by("name"));
        when(clientRepository.findByUserIdAndStatus(userId, ClientStatus.ACTIVE, pageable))
            .thenReturn(new PageImpl<>(List.of(ownedClient()), pageable, 1));
        
        PageResponse<ClientResponse> page = service.list(null, null, pageable);
        
        assertThat(page.content()).hasSize(1);
        assertThat(page.totalElements()).isEqualTo(1);
        assertThat(page.hasNext()).isFalse();
        // Scoped at the query — another user's rows are never fetched (BR-004).
        verify(clientRepository).findByUserIdAndStatus(userId, ClientStatus.ACTIVE, pageable);
    }
    
    @Test
    void listsArchivedClientsWhenAsked() {
        signedIn();
        Pageable pageable = PageRequest.of(0, 20);
        when(clientRepository.findByUserIdAndStatus(userId, ClientStatus.ARCHIVED, pageable))
            .thenReturn(new PageImpl<>(List.of(), pageable, 0));
        
        service.list(ClientStatus.ARCHIVED, null, pageable);
        
        verify(clientRepository).findByUserIdAndStatus(userId, ClientStatus.ARCHIVED, pageable);
    }
    
    @Test
    void filtersByNameWhenQIsSupplied() {
        signedIn();
        Pageable pageable = PageRequest.of(0, 20);
        when(clientRepository.findByUserIdAndStatusAndNameContainingIgnoreCase(
            userId, ClientStatus.ACTIVE, "acme", pageable))
            .thenReturn(new PageImpl<>(List.of(ownedClient()), pageable, 1));
        
        service.list(null, "  acme  ", pageable);
        
        verify(clientRepository).findByUserIdAndStatusAndNameContainingIgnoreCase(
            userId, ClientStatus.ACTIVE, "acme", pageable);
    }
    
    @Test
    void ignoresABlankQ() {
        signedIn();
        Pageable pageable = PageRequest.of(0, 20);
        when(clientRepository.findByUserIdAndStatus(userId, ClientStatus.ACTIVE, pageable))
            .thenReturn(new PageImpl<>(List.of(), pageable, 0));
        
        service.list(null, "   ", pageable);
        
        verify(clientRepository).findByUserIdAndStatus(userId, ClientStatus.ACTIVE, pageable);
        verify(clientRepository, never())
            .findByUserIdAndStatusAndNameContainingIgnoreCase(any(), any(), any(), any());
    }
    
    @Test
    void returnsAnOwnedClient() {
        signedIn();
        Client client = ownedClient();
        when(clientRepository.findById(client.getId())).thenReturn(Optional.of(client));
        
        assertThat(service.get(client.getId()).name()).isEqualTo("Acme Corp");
    }
    
    @Test
    void reportsAnotherUsersClientAsNotFound() {
        signedIn();
        Client someoneElses = Client.create(UUID.randomUUID(), "Theirs", null, null);
        when(clientRepository.findById(someoneElses.getId())).thenReturn(Optional.of(someoneElses));
        
        // BR-004: not 403 — that would confirm the id is real (SECURITY §5).
        assertThat(catchThrowableOfType(() -> service.get(someoneElses.getId()),
            ResourceNotFoundException.class)).isNotNull();
    }
    
    @Test
    void reportsAnUnknownClientAsNotFound() {
        signedIn();
        UUID unknown = UUID.randomUUID();
        when(clientRepository.findById(unknown)).thenReturn(Optional.empty());
        
        assertThat(catchThrowableOfType(() -> service.get(unknown), ResourceNotFoundException.class))
            .isNotNull();
    }
    
    @Test
    void updatesOnlySuppliedFields() {
        signedIn();
        Client client = ownedClient();
        when(clientRepository.findById(client.getId())).thenReturn(Optional.of(client));
        expectSave();
        
        ClientResponse response = service.update(client.getId(),
            new UpdateClientRequest("Acme Holdings", null, null));
        
        assertThat(response.name()).isEqualTo("Acme Holdings");
        assertThat(response.contactDetails()).isEqualTo("acme@example.com");
        assertThat(response.notes()).isEqualTo("Notes");
    }
    
    @Test
    void acceptsAnUpdateThatOmitsTheName() {
        signedIn();
        Client client = ownedClient();
        when(clientRepository.findById(client.getId())).thenReturn(Optional.of(client));
        expectSave();
        
        // Absent name is legal on PATCH (API_SPEC §2.3) even though the name is required on create.
        assertThatCode(() -> service.update(client.getId(), new UpdateClientRequest(null, "new@example.com", null)))
            .doesNotThrowAnyException();
    }
    
    @Test
    void rejectsAnUpdateThatBlanksTheName() {
        signedIn();
        Client client = ownedClient();
        when(clientRepository.findById(client.getId())).thenReturn(Optional.of(client));
        
        // Present-but-blank is not the same as absent — VR-004 still requires a non-empty name.
        var thrown = catchThrowableOfType(
            () -> service.update(client.getId(), new UpdateClientRequest("  ", null, null)),
            com.ledgerai.common.exception.ValidationFailedException.class);
        
        assertThat(thrown.getFieldErrors()).containsOnlyKeys("name");
        verify(clientRepository, never()).save(any());
    }
    
    @Test
    void refusesToUpdateAnotherUsersClient() {
        signedIn();
        Client someoneElses = Client.create(UUID.randomUUID(), "Theirs", null, null);
        when(clientRepository.findById(someoneElses.getId())).thenReturn(Optional.of(someoneElses));
        
        assertThat(catchThrowableOfType(
            () -> service.update(someoneElses.getId(), new UpdateClientRequest("Mine now", null, null)),
            ResourceNotFoundException.class)).isNotNull();
        verify(clientRepository, never()).save(any());
    }
    
    @Test
    void archivesAnOwnedClient() {
        signedIn();
        Client client = ownedClient();
        when(clientRepository.findById(client.getId())).thenReturn(Optional.of(client));
        
        service.archive(client.getId());
        
        assertThat(client.getStatus()).isEqualTo(ClientStatus.ARCHIVED);
        assertThat(client.getArchivedAt()).isNotNull();
    }
    
    @Test
    void archivingIsIdempotent() {
        signedIn();
        Client client = ownedClient();
        when(clientRepository.findById(client.getId())).thenReturn(Optional.of(client));
        
        service.archive(client.getId());
        var archivedAt = client.getArchivedAt();
        service.archive(client.getId());
        
        // API_SPEC §7.5: archiving twice still succeeds and does not move the timestamp.
        assertThat(client.getArchivedAt()).isEqualTo(archivedAt);
    }
    
    @Test
    void refusesToArchiveAnotherUsersClient() {
        signedIn();
        Client someoneElses = Client.create(UUID.randomUUID(), "Theirs", null, null);
        when(clientRepository.findById(someoneElses.getId())).thenReturn(Optional.of(someoneElses));
        
        assertThat(catchThrowableOfType(() -> service.archive(someoneElses.getId()),
            ResourceNotFoundException.class)).isNotNull();
        assertThat(someoneElses.getStatus()).isEqualTo(ClientStatus.ACTIVE);
    }
    
    @Test
    void neverExposesTheOwnerInTheResponse() {
        signedIn();
        expectSave();
        
        // API_SPEC §17.3 lists no userId; the record's components are the contract.
        ClientResponse response = service.create(new CreateClientRequest("Acme Corp", null, null));
        
        assertThat(ClientResponse.class.getRecordComponents())
            .extracting(java.lang.reflect.RecordComponent::getName)
            .containsExactly("id", "name", "contactDetails", "notes", "status", "archivedAt",
                "createdAt", "updatedAt");
        assertThat(response).isNotNull();
    }
}
