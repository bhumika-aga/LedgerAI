package com.ledgerai.ai.adapter;

import com.ledgerai.ai.config.AiProperties;
import com.ledgerai.ai.port.AiCompletion;
import com.ledgerai.ai.port.AiPrompt;
import com.ledgerai.ai.port.AiUnavailableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Adapter tests for {@link AnthropicAiAdapter} against a <strong>mocked</strong> Anthropic Messages API
 * (MockRestServiceServer) — the real provider is never contacted. Verifies the request maps the domain
 * prompt's two channels onto the provider's system/user split with the required headers, that the text
 * block is mapped back to an {@link AiCompletion}, and that any provider/transport error becomes an
 * {@link AiUnavailableException} — so no provider type escapes the port.
 */
class AnthropicAiAdapterTest {
    
    private static final AiProperties PROPERTIES = new AiProperties(
        "https://api.anthropic.com", "2023-06-01", "test-key", "claude-opus-4-8", 1024, 24000);
    
    private RestClient.Builder builder;
    private MockRestServiceServer server;
    private AnthropicAiAdapter adapter;
    
    @BeforeEach
    void setUp() {
        builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        adapter = new AnthropicAiAdapter(builder, PROPERTIES);
    }
    
    @Test
    void mapsThePromptChannelsAndHeadersOntoTheMessagesRequest() {
        server.expect(requestTo("https://api.anthropic.com/v1/messages"))
            .andExpect(method(org.springframework.http.HttpMethod.POST))
            .andExpect(header("x-api-key", "test-key"))
            .andExpect(header("anthropic-version", "2023-06-01"))
            .andExpect(jsonPath("$.model").value("claude-opus-4-8"))
            .andExpect(jsonPath("$.max_tokens").value(1024))
            .andExpect(jsonPath("$.system").value("system instructions"))
            .andExpect(jsonPath("$.messages[0].role").value("user"))
            .andExpect(jsonPath("$.messages[0].content").value("grounded document"))
            .andRespond(withSuccess(
                "{\"content\":[{\"type\":\"text\",\"text\":\"A grounded summary.\"}]}",
                MediaType.APPLICATION_JSON));
        
        AiCompletion completion = adapter.generate(new AiPrompt("system instructions", "grounded document"));
        
        assertThat(completion.text()).isEqualTo("A grounded summary.");
        server.verify();
    }
    
    @Test
    void returnsTheFirstTextBlockWhenSeveralAreReturned() {
        server.expect(requestTo("https://api.anthropic.com/v1/messages"))
            .andRespond(withSuccess(
                "{\"content\":[{\"type\":\"text\",\"text\":\"First.\"},{\"type\":\"text\",\"text\":\"Second.\"}]}",
                MediaType.APPLICATION_JSON));
        
        AiCompletion completion = adapter.generate(new AiPrompt("s", "u"));
        
        assertThat(completion.text()).isEqualTo("First.");
    }
    
    @Test
    void translatesAProviderErrorIntoAiUnavailableException() {
        server.expect(requestTo("https://api.anthropic.com/v1/messages"))
            .andRespond(withServerError());
        
        assertThatThrownBy(() -> adapter.generate(new AiPrompt("s", "u")))
            .isInstanceOf(AiUnavailableException.class);
    }
}
