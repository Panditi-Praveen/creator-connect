package com.creatorconnect.ai.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * AI service settings bound from the {@code app.ai} prefix in
 * {@code application.yml}.
 *
 * <p>Holds the external LLM (OpenAI) connection settings and the maximum
 * number of ranked results returned by {@code /ai/discover}. The API key is
 * read from the {@code OPENAI_API_KEY} environment variable and is never
 * logged or exposed in responses.
 */
@Component
@ConfigurationProperties(prefix = "app.ai")
@Getter
@Setter
public class AiProperties {

    /**
     * Maximum number of ranked results returned by the discovery endpoint.
     */
    private int maxResults = 10;

    /**
     * HTTP request timeout in seconds for calls to the LLM provider.
     */
    private int requestTimeoutSeconds = 60;

    /**
     * Maximum number of automatic retries on transient 429 rate-limit errors.
     * Retries are NOT attempted for quota exhaustion, invalid API key, or server errors.
     */
    private int maxRetries = 2;

    /**
     * External LLM (OpenAI) connection settings.
     */
    private final OpenAi openai = new OpenAi();

    /**
     * OpenAI-compatible chat completion settings.
     */
    @Getter
    @Setter
    public static class OpenAi {

        /**
         * API key from {@code OPENAI_API_KEY}. Blank when not configured.
         */
        private String apiKey;

        /**
         * Model identifier (default {@code gpt-4o-mini}).
         */
        private String model;

        /**
         * Chat completions endpoint URL (overridable for compatible gateways).
         */
        private String url;
    }
}
