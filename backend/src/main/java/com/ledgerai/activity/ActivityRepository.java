package com.ledgerai.activity;

import com.ledgerai.activity.domain.Activity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * Persistence for {@link Activity}. Data access only — no business rules (BACKEND_CODING_STANDARDS §4).
 *
 * <p>Reads are always owner-scoped by {@code userId} (BR-006), so another user's rows are never fetched
 * in the first place; the optional {@code clientId} filter narrows to a per-client view (API_SPEC §15.1).
 * The composite {@code (user_id, created_at DESC)} index (DATABASE §9) backs the default "latest first"
 * ordering. The log is append-only, so there are deliberately no update/delete finders.
 */
public interface ActivityRepository extends JpaRepository<Activity, UUID> {
    
    Page<Activity> findByUserId(UUID userId, Pageable pageable);
    
    Page<Activity> findByUserIdAndClientId(UUID userId, UUID clientId, Pageable pageable);
}
