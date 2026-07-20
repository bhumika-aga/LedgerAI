package com.ledgerai.ai.support;

import com.ledgerai.ai.port.AiCompletion;
import com.ledgerai.ai.port.AiPort;
import com.ledgerai.ai.port.AiPrompt;
import com.ledgerai.ai.port.AiUnavailableException;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Test-profile {@link AiPort} — the in-memory stand-in for the Anthropic adapter, which is absent from
 * the {@code test} profile (ARCHITECTURE §10 — the active adapter is chosen by configuration). Tests
 * therefore <strong>never contact a real AI provider</strong>.
 *
 * <p>Deterministic and controllable: an end-to-end test autowires this bean and sets the next outcome
 * before requesting a summary, so the success/empty/unavailable paths can be exercised without a live
 * provider. It also captures the last {@link AiPrompt} it received so a test can assert on the grounded
 * prompt that reached the port. It is test infrastructure, not a production adapter.
 */
@Component
@Profile("test")
public class InMemoryAiPort implements AiPort {

    private volatile Mode mode = Mode.SUCCESS;
    private volatile String text = "AI-generated summary of the document.";
    private volatile AiPrompt lastPrompt;

    @Override
    public AiCompletion generate(AiPrompt prompt) {
        this.lastPrompt = prompt;
        return switch (mode) {
            case UNAVAILABLE -> throw new AiUnavailableException("AI provider unavailable (test).", null);
            case EMPTY -> new AiCompletion("");
            case SUCCESS -> new AiCompletion(text);
        };
    }

    public void succeedWith(String text) {
        this.mode = Mode.SUCCESS;
        this.text = text;
    }

    public void returnEmpty() {
        this.mode = Mode.EMPTY;
    }

    public void beUnavailable() {
        this.mode = Mode.UNAVAILABLE;
    }

    public AiPrompt lastPrompt() {
        return lastPrompt;
    }

    public void reset() {
        this.mode = Mode.SUCCESS;
        this.text = "AI-generated summary of the document.";
        this.lastPrompt = null;
    }

    public enum Mode {SUCCESS, EMPTY, UNAVAILABLE}
}
