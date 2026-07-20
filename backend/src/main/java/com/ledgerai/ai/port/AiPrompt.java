package com.ledgerai.ai.port;

/**
 * A centrally-assembled, channel-separated prompt in domain terms (AI_ARCHITECTURE §8). It carries the
 * two channels the port needs, kept distinct so the adapter maps them onto the provider's own
 * privileged/untrusted split:
 *
 * <ul>
 *   <li>{@code systemInstructions} — fixed guidance defining role, grounding, honesty, and safety rules
 *       (the System Instructions channel). Never contains document/user text.</li>
 *   <li>{@code groundedUserContent} — the grounded document context plus formatting guidance, treated as
 *       <em>data, not instructions</em> (the Document Context channel). This is where untrusted document
 *       text lives, which — kept in its own channel — is the structural defense against prompt injection
 *       (SECURITY §10, AI_ARCHITECTURE §8).</li>
 * </ul>
 *
 * <p>Provider-neutral: it names no vendor and no vendor concept. The adapter decides how these map onto
 * a specific provider request.
 */
public record AiPrompt(String systemInstructions, String groundedUserContent) {
}
