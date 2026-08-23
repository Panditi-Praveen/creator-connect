package com.creatorconnect.ai.llm;

import com.creatorconnect.ai.config.AiProperties;
import com.creatorconnect.ai.dto.response.TalentResult;
import com.creatorconnect.ai.exception.AiConfigurationException;
import com.creatorconnect.ai.exception.AiLlmException;
import com.creatorconnect.ai.exception.RateLimitException;
import com.creatorconnect.ai.feign.ProfileResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Thin client for the external LLM (OpenAI-compatible chat completions API).
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Building a prompt that asks the model to rank profiles against a
 *       natural-language hiring query.</li>
 *   <li>Requesting strict JSON output and parsing it into {@link TalentResult}s.</li>
 *   <li>Automatically retrying transient {@code 429} rate-limit errors with
 *       exponential backoff (up to {@code maxRetries} attempts). Quota
 *       exhaustion, invalid API keys, and server errors are never retried.</li>
 *   <li>Enforcing a configurable HTTP request timeout to prevent hanging calls.</li>
 *   <li>Failing with {@link AiConfigurationException} when {@code OPENAI_API_KEY}
 *       is missing, and with {@link AiLlmException} on any provider/parse
 *       failure. Fake responses are never produced.</li>
 * </ul>
 *
 * <p>The API key is injected via {@link AiProperties} from the
 * {@code OPENAI_API_KEY} environment variable and is never logged or included
 * in error messages.
 */
@Component
public class LlmClient {

    private static final Logger log = LoggerFactory.getLogger(LlmClient.class);

    private final AiProperties aiProperties;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    /**
     * Creates the client with a configured HTTP timeout.
     *
     * @param aiProperties the {@code app.ai} settings (key, model, url, timeouts, retries)
     * @param objectMapper the Spring-managed Jackson mapper
     */
    public LlmClient(AiProperties aiProperties, ObjectMapper objectMapper) {
        this.aiProperties = aiProperties;
        this.objectMapper = objectMapper;

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        int timeoutMs = aiProperties.getRequestTimeoutSeconds() * 1000;
        factory.setConnectTimeout(timeoutMs);
        factory.setReadTimeout(timeoutMs);
        this.restClient = RestClient.builder()
                .requestFactory(factory)
                .build();
    }

    /**
     * Asks the LLM to rank the given profiles against a hiring query.
     *
     * @param query      the natural-language hiring request
     * @param profiles   the candidate profiles (never {@code null})
     * @param maxResults the maximum number of ranked results to return
     * @return the ranked results, sorted by score descending (may be empty)
     * @throws AiConfigurationException when {@code OPENAI_API_KEY} is not set
     * @throws AiLlmException           when the LLM call or response parsing fails
     * @throws RateLimitException       when rate-limited after all retries are exhausted
     */
    public List<TalentResult> rankProfiles(String query, List<ProfileResponse> profiles, int maxResults) {
        String apiKey = aiProperties.getOpenai().getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new AiConfigurationException(
                    "AI service is not configured: set the OPENAI_API_KEY environment variable");
        }
        String model = aiProperties.getOpenai().getModel();
        String url = aiProperties.getOpenai().getUrl();

        Map<String, Object> requestBody = buildRequestBody(query, profiles, model, maxResults);
        String content = callWithRetry(url, apiKey, requestBody);
        return parseRanking(content, profiles, maxResults);
    }

    /* ------------------------------------------------------------------ */
    /*  Retry orchestration                                                */
    /* ------------------------------------------------------------------ */

    /**
     * Calls the LLM endpoint with automatic retry for transient 429 errors.
     *
     * <p>Retries only when the 429 body indicates {@code rate_limit_exceeded}
     * (transient). Quota exhaustion ({@code insufficient_quota}), invalid API
     * key ({@code invalid_api_key}), and server errors are never retried.
     *
     * <p>Uses exponential backoff: 2s, 4s, … capped at 30s, or the
     * provider's {@code Retry-After} value if larger.
     */
    private String callWithRetry(String url, String apiKey, Map<String, Object> body) {
        int maxAttempts = 1 + aiProperties.getMaxRetries(); // e.g. 1 initial + 2 retries = 3 total

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return callChatCompletions(url, apiKey, body);
            } catch (RateLimitException ex) {
                Integer providerRetryAfter = ex.getRetryAfter();
                String errorType = ex.getErrorType();

                // Only retry transient rate_limit_exceeded — never retry insufficient_quota or invalid_api_key
                boolean isTransient = "rate_limit_exceeded".equals(errorType);
                boolean hasMoreAttempts = attempt < maxAttempts;

                if (isTransient && hasMoreAttempts) {
                    int backoffSeconds = computeBackoff(attempt, providerRetryAfter);
                    log.warn("LLM rate-limited (attempt {}/{}), retrying in {}s [type={}]",
                            attempt, maxAttempts, backoffSeconds, errorType);
                    sleep(backoffSeconds);
                    continue;
                }

                // Non-transient or retries exhausted — propagate to caller
                log.warn("LLM rate-limit not retryable (attempt {}/{}): type={}, transient={}",
                        attempt, maxAttempts, errorType, isTransient);
                throw ex;
            }
        }
        // Should not be reached, but defensive fallback
        throw new AiLlmException("AI service: unexpected retry loop exit", null);
    }

    /**
     * Computes the backoff delay in seconds for the given retry attempt.
     *
     * @param attempt          the current attempt number (1-based)
     * @param providerRetryAfter the provider's suggested retry-after in seconds (may be null)
     * @return the delay in seconds, at least 2s and at most 30s
     */
    private int computeBackoff(int attempt, Integer providerRetryAfter) {
        // Exponential: 2s, 4s, 8s, …
        int exponential = (int) Math.min(30, 2 * Math.pow(2, attempt - 1));
        // Use the larger of exponential backoff and provider hint
        if (providerRetryAfter != null && providerRetryAfter > 0) {
            return Math.max(exponential, Math.min(providerRetryAfter, 60));
        }
        return exponential;
    }

    /**
     * Sleeps for the given seconds, catching and logging any interruption.
     */
    private void sleep(int seconds) {
        try {
            Thread.sleep(Duration.ofSeconds(seconds));
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            log.warn("Retry sleep interrupted");
        }
    }

    /* ------------------------------------------------------------------ */
    /*  HTTP call                                                          */
    /* ------------------------------------------------------------------ */

    /**
     * Invokes the chat completions endpoint and extracts the assistant's text.
     *
     * @param url    the endpoint URL
     * @param apiKey the bearer API key
     * @param body   the JSON request body
     * @return the assistant's message content
     * @throws AiConfigurationException on 401/403 (invalid API key)
     * @throws RateLimitException      on 429 (with parsed error type and retry-after)
     * @throws AiLlmException          on any other provider or connection failure
     */
    private String callChatCompletions(String url, String apiKey, Map<String, Object> body) {
        try {
            ChatCompletionResponse response = restClient.post()
                    .uri(url)
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .body(body)
                    .retrieve()
                    .body(ChatCompletionResponse.class);
            if (response == null || response.choices() == null || response.choices().isEmpty()
                    || response.choices().get(0).message() == null
                    || response.choices().get(0).message().content() == null) {
                throw new AiLlmException("AI service returned an empty response", null);
            }
            return response.choices().get(0).message().content();
        } catch (HttpClientErrorException ex) {
            int status = ex.getStatusCode().value();

            if (status == 401 || status == 403) {
                log.warn("LLM rejected API key (HTTP {})", status);
                throw new AiConfigurationException(
                        "AI service credentials are invalid: check the OPENAI_API_KEY");
            }

            if (status == 429) {
                throw handleRateLimitResponse(ex);
            }

            log.warn("LLM client error (HTTP {}): {}", status, ex.getResponseBodyAsString());
            throw new AiLlmException("AI service returned an error (HTTP " + status + ")", ex);
        } catch (HttpServerErrorException ex) {
            int status = ex.getStatusCode().value();
            log.warn("LLM provider server error (HTTP {}): {}", status, ex.getResponseBodyAsString());
            throw new AiLlmException(
                    "AI service provider error (HTTP " + status + ")", ex);
        } catch (RateLimitException | AiConfigurationException | AiLlmException ex) {
            // Re-throw our own exceptions without wrapping
            throw ex;
        } catch (RestClientException ex) {
            // Includes ResourceAccessException (timeout), ConnectException, etc.
            String msg = ex.getMessage();
            if (msg != null && (msg.contains("timed out") || msg.contains("Read timed out"))) {
                log.warn("LLM call timed out after {}s", aiProperties.getRequestTimeoutSeconds());
                throw new AiLlmException(
                        "AI service request timed out. Please try again.", ex);
            }
            log.warn("LLM call failed: {}", msg);
            throw new AiLlmException("AI service is temporarily unavailable", ex);
        }
    }

    /* ------------------------------------------------------------------ */
    /*  429 response parsing                                               */
    /* ------------------------------------------------------------------ */

    /**
     * Parses a 429 response to extract the error type and retry-after value.
     *
     * <p>OpenAI returns a JSON body like:
     * <pre>
     * {"error": {"message": "...", "type": "rate_limit_exceeded", "code": "rate_limit_exceeded"}}
     * </pre>
     *
     * <p>Error types:
     * <ul>
     *   <li>{@code rate_limit_exceeded} — transient, retryable</li>
     *   <li>{@code insufficient_quota} — billing/quota exhausted, not retryable</li>
     *   <li>{@code invalid_api_key} — auth failure, not retryable</li>
     * </ul>
     */
    private RateLimitException handleRateLimitResponse(HttpClientErrorException ex) {
        Integer retryAfter = parseRetryAfter(ex.getResponseHeaders());
        String errorType = "unknown";
        String providerMessage = null;

        // Try to parse the OpenAI error body for the error type
        String responseBody = ex.getResponseBodyAsString();
        if (responseBody != null && !responseBody.isBlank()) {
            try {
                JsonNode root = objectMapper.readTree(responseBody);
                JsonNode errorNode = root.path("error");
                if (!errorNode.isMissingNode()) {
                    JsonNode typeNode = errorNode.path("type");
                    JsonNode codeNode = errorNode.path("code");
                    JsonNode msgNode = errorNode.path("message");
                    // Prefer "type", fall back to "code"
                    if (!typeNode.isMissingNode()) {
                        errorType = typeNode.asText("unknown");
                    } else if (!codeNode.isMissingNode()) {
                        errorType = codeNode.asText("unknown");
                    }
                    if (!msgNode.isMissingNode()) {
                        providerMessage = msgNode.asText(null);
                    }
                }
            } catch (Exception parseEx) {
                log.debug("Could not parse 429 response body: {}", parseEx.getMessage());
            }
        }

        log.warn("LLM rate-limited: type={}, retryAfter={}s, providerMessage={}",
                errorType, retryAfter, providerMessage);

        // Choose a user-facing message based on the error type
        String message;
        switch (errorType) {
            case "insufficient_quota":
                message = "AI service quota is currently unavailable. Please check your billing or try again later.";
                break;
            case "invalid_api_key":
                // This shouldn't normally be a 429, but handle defensively
                throw new AiConfigurationException(
                        "AI service credentials are invalid: check the OPENAI_API_KEY");
            default:
                // rate_limit_exceeded or unknown — treat as transient
                message = "AI is receiving too many requests. Please wait a moment and try again.";
                break;
        }

        return new RateLimitException(message, retryAfter, errorType);
    }

    /**
     * Extracts the {@code Retry-After} value (in seconds) from the response headers.
     *
     * @param headers the response headers (may be {@code null})
     * @return the retry-after seconds, or {@code null} if not present or unparseable
     */
    private Integer parseRetryAfter(HttpHeaders headers) {
        if (headers == null) return null;
        String value = headers.getFirst("Retry-After");
        if (value == null || value.isBlank()) return null;
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /* ------------------------------------------------------------------ */
    /*  Prompt building                                                    */
    /* ------------------------------------------------------------------ */

    private Map<String, Object> buildRequestBody(String query, List<ProfileResponse> profiles,
                                                 String model, int maxResults) {
        String systemPrompt = "You are a talent-matching assistant for a creative talent platform. "
                + "You match freelancer profiles to a natural-language hiring request. "
                + "Respond with JSON only, in this exact shape: "
                + "{\"results\":[{\"profileId\":\"<uuid>\",\"score\":<number between 0.0 and 1.0>,\"reason\":\"<short reason>\"}]}. "
                + "Return at most " + maxResults + " results, sorted by score descending. "
                + "Only include profiles that plausibly match the request. "
                + "If none of the profiles match, return {\"results\":[]}. "
                + "Never invent profile ids that are not in the provided list.";
        String userPrompt = "Hiring request: " + query
                + "\n\nCandidate profiles (JSON):\n" + toJson(profiles);

        return Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)
                ),
                "temperature", 0.2,
                "response_format", Map.of("type", "json_object")
        );
    }

    /* ------------------------------------------------------------------ */
    /*  Response parsing                                                   */
    /* ------------------------------------------------------------------ */

    private List<TalentResult> parseRanking(String content, List<ProfileResponse> profiles, int maxResults) {
        Map<UUID, ProfileResponse> byId = new HashMap<>();
        for (ProfileResponse profile : profiles) {
            byId.put(profile.getId(), profile);
        }
        try {
            LlmRanking ranking = objectMapper.readValue(content, LlmRanking.class);
            List<TalentResult> results = new ArrayList<>();
            if (ranking.results() != null) {
                for (LlmRanking.LlmResult item : ranking.results()) {
                    if (item.profileId() == null) {
                        continue;
                    }
                    ProfileResponse profile = byId.get(item.profileId());
                    if (profile == null) {
                        continue;
                    }
                    results.add(toTalentResult(profile, item));
                }
            }
            results.sort(Comparator.comparing(TalentResult::getScore,
                    Comparator.nullsLast(Comparator.reverseOrder())));
            if (results.size() > maxResults) {
                return results.subList(0, maxResults);
            }
            return results;
        } catch (JsonProcessingException ex) {
            log.warn("LLM response could not be parsed as the expected JSON ranking");
            throw new AiLlmException("AI service returned an unparseable response", ex);
        }
    }

    private TalentResult toTalentResult(ProfileResponse profile, LlmRanking.LlmResult item) {
        String name = (profile.getFirstName() == null ? "" : profile.getFirstName())
                + (profile.getLastName() == null || profile.getLastName().isBlank()
                        ? "" : " " + profile.getLastName());
        return TalentResult.builder()
                .profileId(profile.getId())
                .userId(profile.getUserId())
                .name(name.isBlank() ? null : name.trim())
                .headline(profile.getHeadline())
                .skills(profile.getSkills())
                .location(profile.getLocation())
                .availableForHire(profile.getAvailableForHire())
                .score(item.score())
                .reason(item.reason())
                .build();
    }

    private String toJson(List<ProfileResponse> profiles) {
        try {
            return objectMapper.writeValueAsString(profiles);
        } catch (JsonProcessingException ex) {
            throw new AiLlmException("AI service could not prepare candidate profiles", ex);
        }
    }

    /* ------------------------------------------------------------------ */
    /*  Internal records                                                   */
    /* ------------------------------------------------------------------ */

    private record ChatCompletionResponse(List<Choice> choices) {
        private record Choice(Message message) {
        }

        private record Message(String content) {
        }
    }

    private record LlmRanking(List<LlmResult> results) {
        private record LlmResult(UUID profileId, Double score, String reason) {
        }
    }
}
