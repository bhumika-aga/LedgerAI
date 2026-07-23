package com.ledgerai.activity.domain;

/**
 * The documented action types recorded on the timeline (DATABASE §5.8; API_SPEC §7.3, §8.1, §8.4, §10.1,
 * §13.1). These are exactly the events the <strong>already-implemented</strong> modules are documented to
 * emit — client creation, document upload, document delete, AI summary generation, and report creation.
 *
 * <p>The documentation lists further types for capabilities that are not built yet ({@code EMAIL_GENERATED}
 * and chat activity); they are deliberately absent here because no module emits them, and nothing is
 * invented beyond what is documented. {@code action_type} is a free-text column with <strong>no</strong>
 * {@code CHECK} constraint (DATABASE §5.8), so a future capability can add a value without a schema change.
 */
public enum ActivityType {
    CLIENT_CREATED,
    DOCUMENT_UPLOADED,
    DOCUMENT_DELETED,
    SUMMARY_GENERATED,
    REPORT_CREATED
}
