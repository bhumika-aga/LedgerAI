package com.ledgerai.reports;

import com.ledgerai.reports.domain.Report;
import com.ledgerai.reports.domain.ReportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

/**
 * Persistence for {@link Report}. Data access only — no business rules (BACKEND_CODING_STANDARDS §4).
 *
 * <p>The list finder is owner-scoped by {@code userId} at the query (API_SPEC §13.2 — "Owner-scoped"), so
 * another user's rows are never fetched; the optional {@code documentId} and {@code status} filters are
 * applied inline. Reads/writes by id go through the shared {@code OwnershipGuard} in the service (a
 * non-owned/unknown report is {@code 404}), so no ownership logic is duplicated here.
 */
public interface ReportRepository extends JpaRepository<Report, UUID> {
    
    @Query("""
        SELECT r FROM Report r
        WHERE r.userId = :userId
          AND (:documentId IS NULL OR r.documentId = :documentId)
          AND (:status IS NULL OR r.status = :status)
        """)
    Page<Report> findOwned(@Param("userId") UUID userId,
                           @Param("documentId") UUID documentId,
                           @Param("status") ReportStatus status,
                           Pageable pageable);
}
