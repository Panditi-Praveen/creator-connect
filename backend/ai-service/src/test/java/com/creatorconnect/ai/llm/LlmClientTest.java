package com.creatorconnect.ai.llm;

import com.creatorconnect.ai.config.AiProperties;
import com.creatorconnect.ai.exception.AiConfigurationException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link LlmClient}.
 *
 * <p>Covers the configuration guard: when {@code OPENAI_API_KEY} is missing
 * or blank the client must fail fast with {@link AiConfigurationException}
 * (mapped to {@code 503}) before any HTTP call — no fake responses are ever
 * produced. The provider/parse failure paths are covered end-to-end (mapped
 * to {@code 502}) by the controller tests and integration testing.
 */
class LlmClientTest {

    @Test
    void rankProfiles_whenApiKeyMissing_throwsAiConfigurationException() {
        AiProperties aiProperties = new AiProperties();
        aiProperties.setMaxResults(10);
        aiProperties.getOpenai().setApiKey(null);

        LlmClient llmClient = new LlmClient(aiProperties, new ObjectMapper());

        assertThatThrownBy(() -> llmClient.rankProfiles("find a designer", List.of(), 10))
                .isInstanceOf(AiConfigurationException.class)
                .hasMessageContaining("OPENAI_API_KEY");
    }

    @Test
    void rankProfiles_whenApiKeyBlank_throwsAiConfigurationException() {
        AiProperties aiProperties = new AiProperties();
        aiProperties.setMaxResults(10);
        aiProperties.getOpenai().setApiKey("   ");

        LlmClient llmClient = new LlmClient(aiProperties, new ObjectMapper());

        assertThatThrownBy(() -> llmClient.rankProfiles("find a designer", List.of(), 10))
                .isInstanceOf(AiConfigurationException.class)
                .hasMessageContaining("OPENAI_API_KEY");
    }
}
