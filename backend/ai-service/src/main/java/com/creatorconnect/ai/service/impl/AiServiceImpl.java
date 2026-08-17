package com.creatorconnect.ai.service.impl;

import com.creatorconnect.ai.config.AiProperties;
import com.creatorconnect.ai.dto.response.DiscoverResponse;
import com.creatorconnect.ai.dto.response.StatusResponse;
import com.creatorconnect.ai.dto.response.TalentResult;
import com.creatorconnect.ai.feign.ProfileClientService;
import com.creatorconnect.ai.feign.ProfileResponse;
import com.creatorconnect.ai.llm.LlmClient;
import com.creatorconnect.ai.service.AiService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Concrete {@link AiService} implementation.
 *
 * <p>Discovery flow:
 * <ol>
 *   <li>Fetch the talent pool from the Profile Service (via OpenFeign,
 *       forwarding the caller's JWT). A Profile Service outage surfaces as
 *       {@code 503} — never a fake result.</li>
 *   <li>If there are no profiles, return an empty result set without calling
 *       the LLM (graceful no-op).</li>
 *   <li>Otherwise ask the LLM to rank the profiles against the query and
 *       convert the structured ranking into {@link TalentResult}s.</li>
 * </ol>
 *
 * <p>No database, no own entities — the architecture explicitly forbids them.
 */
@Service
public class AiServiceImpl implements AiService {

    private final ProfileClientService profileClientService;
    private final LlmClient llmClient;
    private final AiProperties aiProperties;

    /**
     * Creates the service with its collaborators.
     *
     * @param profileClientService the Profile Service client façade
     * @param llmClient            the external LLM client
     * @param aiProperties         the {@code app.ai} settings (result cap)
     */
    public AiServiceImpl(ProfileClientService profileClientService,
                         LlmClient llmClient,
                         AiProperties aiProperties) {
        this.profileClientService = profileClientService;
        this.llmClient = llmClient;
        this.aiProperties = aiProperties;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DiscoverResponse discover(String query) {
        List<ProfileResponse> profiles = profileClientService.fetchFreelancerProfiles();
        if (profiles.isEmpty()) {
            return DiscoverResponse.builder()
                    .query(query)
                    .count(0)
                    .results(List.of())
                    .build();
        }
        List<TalentResult> results = llmClient.rankProfiles(query, profiles, aiProperties.getMaxResults());
        return DiscoverResponse.builder()
                .query(query)
                .count(results.size())
                .results(results)
                .build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StatusResponse status() {
        return new StatusResponse("ai-service", "UP");
    }
}
