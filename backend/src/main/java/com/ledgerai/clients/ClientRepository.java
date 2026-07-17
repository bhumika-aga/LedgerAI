package com.ledgerai.clients;

import com.ledgerai.clients.domain.Client;
import com.ledgerai.clients.domain.ClientStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * Persistence for {@link Client}. Data access only — no business rules (BACKEND_CODING_STANDARDS §4).
 *
 * <p>Both finders are <strong>owner-scoped by construction</strong>: there is deliberately no
 * "find all clients" method, so no caller can accidentally read across users (BACKEND_CODING_STANDARDS
 * §4 — "produce owner-scoped queries"; BR-004). Both also take a status, so the read path always
 * filters the soft-delete column rather than relying on the caller to remember (DATABASE §8). Both are
 * served by the composite `(user_id, status)` index (DATABASE §9).
 */
public interface ClientRepository extends JpaRepository<Client, UUID> {
    
    Page<Client> findByUserIdAndStatus(UUID userId, ClientStatus status, Pageable pageable);
    
    Page<Client> findByUserIdAndStatusAndNameContainingIgnoreCase(
        UUID userId, ClientStatus status, String name, Pageable pageable);
}
