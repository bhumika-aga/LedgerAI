package com.ledgerai.common.security;

import java.util.UUID;

/**
 * The authenticated user, expressed in application terms (ARCHITECTURE §7.1, SECURITY §5).
 *
 * <p>It carries only the identity needed to authorize a request — the user id — mirroring the access
 * token's minimal claim set (SECURITY §7, ADR-001). It deliberately holds no roles, permissions,
 * email, or other PII: authorization in LedgerAI is ownership-based, not role-based (SECURITY §5,
 * §20), and role/permission models are an explicitly future concern (SECURITY §19).
 *
 * <p>This type exists so business modules can name the caller without importing Spring Security or
 * touching JWT internals (BACKEND_CODING_STANDARDS §4 — services must not depend on web/HTTP types).
 */
public record CurrentUser(UUID id) {
    
    public CurrentUser {
        if (id == null) {
            // Fail closed: a principal without an identity is never a valid principal (SECURITY §5).
            throw new IllegalArgumentException("CurrentUser id must not be null");
        }
    }
}
