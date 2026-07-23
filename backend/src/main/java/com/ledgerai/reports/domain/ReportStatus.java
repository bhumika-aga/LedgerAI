package com.ledgerai.reports.domain;

/**
 * The Report lifecycle states persisted as rows (DATABASE §5.7, SRS §7.3): {@code DRAFT | SAVED}. Export is
 * a client-side action, not a stored state (API_SPEC §13.4). The database {@code CHECK(status IN
 * ('DRAFT','SAVED'))} guards the same set.
 */
public enum ReportStatus {
    DRAFT,
    SAVED
}
