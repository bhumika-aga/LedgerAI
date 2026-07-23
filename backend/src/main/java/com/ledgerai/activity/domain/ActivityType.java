package com.ledgerai.activity.domain;

/**
 * The documented action types recorded on the timeline (DATABASE §5.8; API_SPEC §7.3, §8.1, §8.4, §10.1,
 * §11.1, §13.1). These are the events the <strong>already-implemented</strong> modules are documented to
 * emit — client creation, document upload, document delete, AI summary generation, report creation, and a
 * chat exchange.
 *
 * <p><strong>{@code CHAT_MESSAGE_SENT} naming (documentation gap).</strong> API_SPEC §11.1 requires AI
 * Chat to "emit chat activity" and DATABASE §11 places that Activity insert in the AI-generation atomic
 * unit, but <em>no</em> frozen document names the chat {@code action_type} string (DATABASE §5.8 lists
 * action types only as {@code e.g.} examples and applies <strong>no</strong> {@code CHECK} constraint, so
 * the set is deliberately open). The behavior is mandated; only the label is unspecified. This value
 * follows the established {@code <ENTITY>_<PAST-PARTICIPLE>} convention of the other events rather than
 * inventing new behavior — see the engineering note in IMPLEMENTATION_STATUS.
 *
 * <p>Unlike the chat label, {@code EMAIL_GENERATED} <em>is</em> named explicitly by the documentation
 * (DATABASE §5.8 examples; API_SPEC §12.1 "Emits {@code EMAIL_GENERATED}"), so it introduces no naming
 * decision. {@code action_type} has no DB {@code CHECK} (DATABASE §5.8), so adding a value needs no schema
 * change.
 */
public enum ActivityType {
    CLIENT_CREATED,
    DOCUMENT_UPLOADED,
    DOCUMENT_DELETED,
    SUMMARY_GENERATED,
    CHAT_MESSAGE_SENT,
    EMAIL_GENERATED,
    REPORT_CREATED
}
