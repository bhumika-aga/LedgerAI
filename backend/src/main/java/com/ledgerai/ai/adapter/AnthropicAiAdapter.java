package com.ledgerai.ai.adapter;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.ledgerai.ai.config.AiProperties;
import com.ledgerai.ai.port.AiCompletion;
import com.ledgerai.ai.port.AiPort;
import com.ledgerai.ai.port.AiPrompt;
import com.ledgerai.ai.port.AiUnavailableException;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Map;

/**
 * Anthropic adapter (ADR-003) implementing the domain {@link AiPort} over the Anthropic Messages API.
 *
 * <p>This is the <strong>only</strong> place that knows the provider: the {@code /v1/messages} endpoint,
 * its request/response JSON, the {@code x-api-key}/{@code anthropic-version} headers, the model id, and
 * the API key all live here, and <strong>no provider type crosses the port</strong> — callers see only
 * {@link AiCompletion} / {@link AiUnavailableException}. The active adapter is chosen by configuration
 * (ARCHITECTURE §10, AI_ARCHITECTURE §6); this one is absent from the {@code test} profile, where an
 * in-memory AI port stands in, so tests never contact a real provider.
 *
 * <p>The domain prompt's two channels map onto the provider's privileged/untrusted split: the system
 * instructions become the request {@code system} field, and the grounded (untrusted) document content
 * becomes the {@code user} message — the same channel separation the prompt architecture defines
 * (AI_ARCHITECTURE §8, SECURITY §10). Only prompt content is sent — no account/client metadata
 * (minimum-necessary, NFR-018). Any provider/transport failure becomes {@link AiUnavailableException};
 * the underlying error is not leaked.
 */
@Component
@Profile("!test")
@EnableConfigurationProperties(AiProperties.class)
public class AnthropicAiAdapter implements AiPort {
    
    private final RestClient restClient;
    private final AiProperties properties;
    
    public AnthropicAiAdapter(RestClient.Builder builder, AiProperties properties) {
        this.properties = properties;
        this.restClient = builder.baseUrl(properties.apiUrl()).build();
    }
    
    @Override
    public AiCompletion generate(AiPrompt prompt) {
        Map<String, Object> request = Map.of(
            "model", properties.model(),
            "max_tokens", properties.maxTokens(),
            "system", prompt.systemInstructions(),
            "messages", List.of(Map.of(
                "role", "user",
                "content", prompt.groundedUserContent())));
        try {
            MessagesResponse response = restClient.post()
                                            .uri("/v1/messages")
                                            .header("x-api-key", properties.apiKey())
                                            .header("anthropic-version", properties.apiVersion())
                                            .contentType(MediaType.APPLICATION_JSON)
                                            .body(request)
                                            .retrieve()
                                            .body(MessagesResponse.class);
            return new AiCompletion(firstText(response));
        } catch (RestClientException e) {
            throw new AiUnavailableException("The AI service is currently unavailable. Please try again.", e);
        }
    }
    
    private String firstText(MessagesResponse response) {
        if (response == null || response.content() == null) {
            return "";
        }
        return response.content().stream()
                   .filter(block -> block != null && "text".equals(block.type()) && block.text() != null)
                   .map(ContentBlock::text)
                   .findFirst()
                   .orElse("");
    }
    
    // --- Minimal Anthropic Messages response shape (provider detail confined to this adapter) ---------
    
    @JsonIgnoreProperties(ignoreUnknown = true)
    private record MessagesResponse(List<ContentBlock> content) {
    }
    
    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ContentBlock(String type, String text) {
    }
}
