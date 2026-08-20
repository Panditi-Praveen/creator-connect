package com.creatorconnect.ai.llm;

import com.creatorconnect.ai.config.AiProperties;
import com.creatorconnect.ai.dto.response.TalentResult;
import com.creatorconnect.ai.exception.AiConfigurationException;
import com.creatorconnect.ai.exception.AiLlmException;
import com.creatorconnect.ai.feign.ProfileResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

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
 *   <li>Builds a prompt that asks the model to rank the given profiles
 *       against a natural-language hiring query.</li>
 *   <li>Requests strict JSON output and parses it into {@link TalentResult}s.</li>
 *   <li>Never returns raw LLM text as the API contract — only the structured
 *       ranking is exposed.</li>
 *   <li>Fails with {@link AiConfigurationException} when {@code OPENAI_API_KEY}
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
     * Creates the client.
     *
     * @param aiProperties the {@code app.ai} settings (key, model, url)
     * @param objectMapper the Spring-managed Jackson mapper
     */
    public LlmClient(AiProperties aiProperties, ObjectMapper objectMapper) {
        this.aiProperties = aiProperties;
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder().build();
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
        String content = callChatCompletions(url, apiKey, requestBody);
        return parseRanking(content, profiles, maxResults);
    }

    /**
     * Builds the chat completions request body.
     *
     * @param query      the hiring query
     * @param profiles   the candidate profiles
     * @param model      the configured model identifier
     * @param maxResults the result cap (also told to the model)
     * @return the JSON-serializable request body
     */
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

    /**
     * Invokes the chat completions endpoint and extracts the assistant's text.
     *
     * @param url    the endpoint URL
     * @param apiKey the bearer API key
     * @param body   the JSON request body
     * @return the assistant's message content
     * @throws AiLlmException on any provider or connection failure
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
                log.warn("LLM rate-limited (HTTP 429)");
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                        "AI service rate limit exceeded — please try again later");
            }
            log.warn("LLM client error (HTTP {}): {}", status, ex.getMessage());
            throw new AiLlmException("AI service returned an error (HTTP " + status + ")", ex);
        } catch (HttpServerErrorException ex) {
            int status = ex.getStatusCode().value();
            log.warn("LLM provider error (HTTP {}): {}", status, ex.getMessage());
            throw new AiLlmException(
                    "AI service provider error (HTTP " + status + ")", ex);
        } catch (RestClientException ex) {
            log.warn("LLM call failed: {}", ex.getMessage());
            throw new AiLlmException("AI service is temporarily unavailable", ex);
        }
    }

    /**
     * Parses the model's strict-JSON ranking into {@link TalentResult}s,
     * resolving each returned id against the candidate profiles.
     *
     * @param content    the raw assistant text (must be JSON)
     * @param profiles   the candidate profiles used to resolve ids
     * @param maxResults the result cap
     * @return the ranked results sorted by score descending
     * @throws AiLlmException when the content is not the expected JSON
     */
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
                        // The model returned an id that was not in the candidate list — ignore it.
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

    /**
     * Maps a candidate profile plus the model's ranking entry into a
     * {@link TalentResult}.
     *
     * @param profile the resolved candidate profile
     * @param item    the model's ranking entry
     * @return the combined result
     */
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

    /**
     * Serializes the candidate profiles for the prompt.
     *
     * @param profiles the candidate profiles
     * @return a compact JSON string
     */
    private String toJson(List<ProfileResponse> profiles) {
        try {
            return objectMapper.writeValueAsString(profiles);
        } catch (JsonProcessingException ex) {
            throw new AiLlmException("AI service could not prepare candidate profiles", ex);
        }
    }

    /**
     * Minimal projection of the OpenAI chat completions response.
     */
    private record ChatCompletionResponse(List<Choice> choices) {
        private record Choice(Message message) {
        }

        private record Message(String content) {
        }
    }

    /**
     * Strict-JSON ranking returned by the model.
     */
    private record LlmRanking(List<LlmResult> results) {
        private record LlmResult(UUID profileId, Double score, String reason) {
        }
    }
}
