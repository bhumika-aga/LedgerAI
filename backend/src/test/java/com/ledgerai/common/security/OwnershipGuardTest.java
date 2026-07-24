package com.ledgerai.common.security;

import com.ledgerai.common.exception.ResourceNotFoundException;
import com.ledgerai.common.exception.UnauthenticatedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * Unit tests for ownership validation (SECURITY §5, BR-004) — the primary confidentiality control.
 *
 * <p>The central assertion throughout is that a resource the caller does not own is reported exactly
 * like one that does not exist: same exception, same message. Anything that distinguishes them leaks
 * existence, which is precisely what the {@code 404} policy exists to prevent.
 */
@ExtendWith(MockitoExtension.class)
class OwnershipGuardTest {
    
    @Mock
    private CurrentUserProvider currentUserProvider;
    private OwnershipGuard guard;
    private UUID currentUserId;
    
    @BeforeEach
    void setUp() {
        guard = new OwnershipGuard(currentUserProvider);
        currentUserId = UUID.randomUUID();
    }
    
    private void signedIn() {
        when(currentUserProvider.requireUserId()).thenReturn(currentUserId);
    }
    
    @Test
    void requireOwnerAcceptsTheOwner() {
        signedIn();
        
        assertThatCode(() -> guard.requireOwner(currentUserId)).doesNotThrowAnyException();
    }
    
    @Test
    void requireOwnerRejectsAnotherUsersResourceAsNotFound() {
        signedIn();
        
        assertThatThrownBy(() -> guard.requireOwner(UUID.randomUUID()))
            .isInstanceOf(ResourceNotFoundException.class);
    }
    
    @Test
    void requireOwnerFailsClosedOnAMissingOwner() {
        signedIn();
        
        assertThatThrownBy(() -> guard.requireOwner(null)).isInstanceOf(ResourceNotFoundException.class);
    }
    
    @Test
    void requireOwnerRequiresAnAuthenticatedUser() {
        when(currentUserProvider.requireUserId()).thenThrow(new UnauthenticatedException());
        
        assertThatThrownBy(() -> guard.requireOwner(currentUserId))
            .isInstanceOf(UnauthenticatedException.class);
    }
    
    @Test
    void requireOwnedReturnsTheResourceWhenOwned() {
        signedIn();
        OwnedThing owned = new OwnedThing(currentUserId);
        
        assertThat(guard.requireOwned(Optional.of(owned), OwnedThing::ownerId)).isSameAs(owned);
    }
    
    @Test
    void requireOwnedRejectsAnotherUsersResource() {
        signedIn();
        OwnedThing someoneElses = new OwnedThing(UUID.randomUUID());
        
        assertThatThrownBy(() -> guard.requireOwned(Optional.of(someoneElses), OwnedThing::ownerId))
            .isInstanceOf(ResourceNotFoundException.class);
    }
    
    @Test
    void requireOwnedRejectsAnAbsentResource() {
        signedIn();
        
        assertThatThrownBy(() -> guard.requireOwned(Optional.empty(), OwnedThing::ownerId))
            .isInstanceOf(ResourceNotFoundException.class);
    }
    
    @Test
    void reportsNonOwnedAndAbsentResourcesIdentically() {
        signedIn();
        
        Throwable nonOwned = catchThrowable(
            () -> guard.requireOwned(Optional.of(new OwnedThing(UUID.randomUUID())), OwnedThing::ownerId));
        Throwable absent = catchThrowable(
            () -> guard.requireOwned(Optional.empty(), OwnedThing::ownerId));
        
        // Indistinguishable by type and by message — no existence is disclosed (SECURITY §5).
        assertThat(nonOwned).isInstanceOf(ResourceNotFoundException.class);
        assertThat(absent).isInstanceOf(ResourceNotFoundException.class);
        assertThat(nonOwned.getMessage()).isEqualTo(absent.getMessage());
    }
    
    @Test
    void isOwnerReportsOwnershipWithoutThrowing() {
        signedIn();
        
        assertThat(guard.isOwner(currentUserId)).isTrue();
        assertThat(guard.isOwner(UUID.randomUUID())).isFalse();
        assertThat(guard.isOwner(null)).isFalse();
    }
    
    /**
     * A resource shaped like the owned entities future slices will pass in; not a domain type.
     */
    private record OwnedThing(UUID ownerId) {
    }
}
