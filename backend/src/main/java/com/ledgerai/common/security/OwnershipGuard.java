package com.ledgerai.common.security;

import com.ledgerai.common.exception.ResourceNotFoundException;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

/**
 * The shared ownership check every protected operation runs before acting (SECURITY §5, BR-004).
 *
 * <p>Authorization in LedgerAI is <strong>ownership-based</strong>: a user may act only on their own
 * data, and ownership is verified in the <strong>service layer</strong> — never inferred from URL
 * structure (ARCHITECTURE §7.1, §9.2; SECURITY §5). This guard is the single reusable expression of
 * that rule so it is applied identically by every module (SECURITY §Engineering Practices).
 *
 * <p><strong>Why everything fails as {@code 404}:</strong> a resource that does not exist and a
 * resource the caller does not own are reported identically. Answering {@code 403} for a non-owned
 * resource would confirm that the id is real, leaking existence; {@code 404} reveals nothing
 * (SECURITY §5, API_SPEC §2.4). {@code 403} is reserved for the case where the caller already
 * legitimately knows the resource exists — see {@link com.ledgerai.common.exception.ForbiddenException}.
 */
@Component
public class OwnershipGuard {
    
    private final CurrentUserProvider currentUserProvider;
    
    public OwnershipGuard(CurrentUserProvider currentUserProvider) {
        this.currentUserProvider = currentUserProvider;
    }
    
    /**
     * Asserts the authenticated user is {@code ownerId}. Use when the owner id is already in hand
     * (e.g. read from the resource being acted on).
     *
     * @throws ResourceNotFoundException                              if the caller is not the owner (non-revealing, SECURITY §5)
     * @throws com.ledgerai.common.exception.UnauthenticatedException if there is no authenticated user
     */
    public void requireOwner(UUID ownerId) {
        UUID currentUserId = currentUserProvider.requireUserId();
        if (ownerId == null || !ownerId.equals(currentUserId)) {
            throw new ResourceNotFoundException();
        }
    }
    
    /**
     * Resolves a lookup result to an owned resource, collapsing "absent" and "not owned" into the same
     * {@code 404} so the two are indistinguishable to a caller (SECURITY §5).
     *
     * <p>Intended use from a service:
     * {@code Client client = ownershipGuard.requireOwned(clientRepository.findById(id), Client::getUserId);}
     *
     * @param resource       the lookup result, possibly empty
     * @param ownerExtractor reads the owning user id from the resource
     * @return the resource, once proven to belong to the authenticated user
     * @throws ResourceNotFoundException                              if absent or not owned by the caller
     * @throws com.ledgerai.common.exception.UnauthenticatedException if there is no authenticated user
     */
    public <T> T requireOwned(Optional<T> resource, Function<T, UUID> ownerExtractor) {
        UUID currentUserId = currentUserProvider.requireUserId();
        T found = resource.orElseThrow(ResourceNotFoundException::new);
        UUID ownerId = ownerExtractor.apply(found);
        if (ownerId == null || !ownerId.equals(currentUserId)) {
            throw new ResourceNotFoundException();
        }
        return found;
    }
    
    /**
     * Non-throwing ownership test, for callers that must branch rather than fail (e.g. filtering).
     * Requires an authenticated user; ownership itself is reported as a boolean.
     */
    public boolean isOwner(UUID ownerId) {
        return ownerId != null && ownerId.equals(currentUserProvider.requireUserId());
    }
}
