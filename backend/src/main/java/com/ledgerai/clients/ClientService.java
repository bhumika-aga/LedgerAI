package com.ledgerai.clients;

import com.ledgerai.clients.config.ClientProperties;
import com.ledgerai.clients.domain.Client;
import com.ledgerai.clients.domain.ClientStatus;
import com.ledgerai.clients.dto.ClientResponse;
import com.ledgerai.clients.dto.CreateClientRequest;
import com.ledgerai.clients.dto.UpdateClientRequest;
import com.ledgerai.common.dto.PageResponse;
import com.ledgerai.common.exception.ValidationFailedException;
import com.ledgerai.common.security.CurrentUserProvider;
import com.ledgerai.common.security.OwnershipGuard;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Client business rules (SRS §4.3: FR-CLNT-001…005; VR-004; BR-001/002/003).
 *
 * <p>Ownership is enforced here, in the service layer, never inferred from the URL (ARCHITECTURE §7.1,
 * §9.2; SECURITY §5). It arrives two ways, and both come from the token, never the request:
 * <ul>
 *   <li><strong>Writes/reads by id</strong> go through {@link OwnershipGuard#requireOwned}, so a client
 *       the caller does not own is indistinguishable from one that does not exist — both {@code 404}.</li>
 *   <li><strong>Listing</strong> is scoped at the query, via owner-scoped repository finders, so
 *       another user's rows are never fetched in the first place.</li>
 * </ul>
 */
@Service
public class ClientService {
    
    private final ClientRepository clientRepository;
    private final CurrentUserProvider currentUserProvider;
    private final OwnershipGuard ownershipGuard;
    private final ClientProperties properties;
    
    public ClientService(ClientRepository clientRepository, CurrentUserProvider currentUserProvider,
                         OwnershipGuard ownershipGuard, ClientProperties properties) {
        this.clientRepository = clientRepository;
        this.currentUserProvider = currentUserProvider;
        this.ownershipGuard = ownershipGuard;
        this.properties = properties;
    }
    
    /**
     * FR-CLNT-002: the caller's clients, filtered and paged (API_SPEC §7.1).
     */
    @Transactional(readOnly = true)
    public PageResponse<ClientResponse> list(ClientStatus status, String q, Pageable pageable) {
        UUID userId = currentUserProvider.requireUserId();
        // "Defaults to ACTIVE" (API_SPEC §7.1) — the read path always filters the soft-delete column
        // rather than trusting the caller to pass one (DATABASE §8).
        ClientStatus effectiveStatus = status == null ? ClientStatus.ACTIVE : status;
        Page<Client> page = (q == null || q.isBlank())
                                ? clientRepository.findByUserIdAndStatus(userId, effectiveStatus, pageable)
                                : clientRepository.findByUserIdAndStatusAndNameContainingIgnoreCase(
            userId, effectiveStatus, q.trim(), pageable);
        return PageResponse.from(page, ClientResponse::from);
    }
    
    /**
     * FR-CLNT-002: a single client the caller owns (API_SPEC §7.2).
     */
    @Transactional(readOnly = true)
    public ClientResponse get(UUID clientId) {
        return ClientResponse.from(requireOwnedClient(clientId));
    }
    
    /**
     * FR-CLNT-001: create a client owned by the caller (API_SPEC §7.3).
     */
    @Transactional
    public ClientResponse create(CreateClientRequest request) {
        UUID userId = currentUserProvider.requireUserId();
        Map<String, String> errors = new LinkedHashMap<>();
        checkName(errors, request.name(), true);
        checkOptionalFields(errors, request.contactDetails(), request.notes());
        throwIfInvalid(errors);
        
        // Duplicate names are allowed and are not an error (BR-024, API_SPEC §7.3), so there is no
        // uniqueness check here — two real clients may legitimately share a name.
        Client client = Client.create(userId, request.name(), request.contactDetails(), request.notes());
        return ClientResponse.from(clientRepository.save(client));
    }
    
    /**
     * FR-CLNT-003: edit a client the caller owns (API_SPEC §7.4).
     */
    @Transactional
    public ClientResponse update(UUID clientId, UpdateClientRequest request) {
        Client client = requireOwnedClient(clientId);
        Map<String, String> errors = new LinkedHashMap<>();
        checkName(errors, request.name(), false);
        checkOptionalFields(errors, request.contactDetails(), request.notes());
        throwIfInvalid(errors);
        
        client.applyUpdate(request.name(), request.contactDetails(), request.notes());
        return ClientResponse.from(clientRepository.save(client));
    }
    
    /**
     * FR-CLNT-004: archive a client the caller owns — soft, idempotent (API_SPEC §7.5, DATABASE §8).
     */
    @Transactional
    public void archive(UUID clientId) {
        requireOwnedClient(clientId).archive();
    }
    
    private Client requireOwnedClient(UUID clientId) {
        return ownershipGuard.requireOwned(clientRepository.findById(clientId), Client::getUserId);
    }
    
    /**
     * VR-004: the name is required and non-empty on create; on update it may be absent, but if supplied
     * it must still be non-empty. Either way it must fit the configured limit.
     */
    private void checkName(Map<String, String> errors, String name, boolean required) {
        if (name == null) {
            if (required) {
                errors.put("name", "Client name is required.");
            }
            return;
        }
        if (name.isBlank()) {
            errors.put("name", "Client name is required.");
        } else if (name.length() > properties.maxNameLength()) {
            errors.put("name", "Must be at most " + properties.maxNameLength() + " characters.");
        }
    }
    
    /**
     * VR-004: "optional fields validated if present".
     */
    private void checkOptionalFields(Map<String, String> errors, String contactDetails, String notes) {
        checkLength(errors, "contactDetails", contactDetails, properties.maxContactDetailsLength());
        checkLength(errors, "notes", notes, properties.maxNotesLength());
    }
    
    private void checkLength(Map<String, String> errors, String field, String value, int maxLength) {
        if (value != null && value.length() > maxLength) {
            errors.put(field, "Must be at most " + maxLength + " characters.");
        }
    }
    
    private void throwIfInvalid(Map<String, String> errors) {
        if (!errors.isEmpty()) {
            // Every failure at once: VR-004 requires field-level messages, and a user should not have to
            // fix one field per round trip.
            throw new ValidationFailedException(errors);
        }
    }
}
