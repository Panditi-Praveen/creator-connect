package com.creatorconnect.ai.service.impl;

import com.creatorconnect.ai.config.AiProperties;
import com.creatorconnect.ai.dto.response.DiscoverResponse;
import com.creatorconnect.ai.dto.response.StatusResponse;
import com.creatorconnect.ai.dto.response.TalentResult;
import com.creatorconnect.ai.feign.ProfileClientService;
import com.creatorconnect.ai.feign.ProfileResponse;
import com.creatorconnect.ai.llm.LlmClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link AiServiceImpl} — the discovery flow: fetch the talent
 * pool from the Profile Service, skip the LLM entirely when the pool is
 * empty, otherwise delegate the ranking to {@link LlmClient}.
 */
@ExtendWith(MockitoExtension.class)
class AiServiceImplTest {

    private static final UUID PROFILE_ID = UUID.fromString("8dade014-d0c9-470d-acb8-744a3712fc76");
    private static final UUID USER_ID = UUID.fromString("f64af15f-01e6-47ff-9d0c-06d55ee21f46");
    private static final String QUERY = "find a video editor";

    @Mock
    private ProfileClientService profileClientService;

    @Mock
    private LlmClient llmClient;

    private AiProperties aiProperties;
    private AiServiceImpl aiService;

    @BeforeEach
    void setUp() {
        aiProperties = new AiProperties();
        aiProperties.setMaxResults(10);
        aiService = new AiServiceImpl(profileClientService, llmClient, aiProperties);
    }

    @Test
    void discover_whenNoProfilesExist_returnsEmptyWithoutCallingLlm() {
        when(profileClientService.fetchFreelancerProfiles()).thenReturn(List.of());

        DiscoverResponse response = aiService.discover(QUERY);

        assertThat(response.getQuery()).isEqualTo(QUERY);
        assertThat(response.getCount()).isZero();
        assertThat(response.getResults()).isEmpty();
        verify(llmClient, never()).rankProfiles(anyString(), anyList(), anyInt());
    }

    @Test
    void discover_whenProfilesExist_delegatesRankingToLlm() {
        ProfileResponse profile = new ProfileResponse(
                PROFILE_ID, USER_ID, "Alex", "Rivera", "Senior Video Editor",
                null, null, "Mumbai, India", null, null, null,
                "Video Editing, After Effects", 6, true, null, null);
        List<ProfileResponse> profiles = List.of(profile);
        when(profileClientService.fetchFreelancerProfiles()).thenReturn(profiles);
        when(llmClient.rankProfiles(QUERY, profiles, 10)).thenReturn(List.of(
                TalentResult.builder()
                        .profileId(PROFILE_ID)
                        .userId(USER_ID)
                        .name("Alex Rivera")
                        .score(0.95)
                        .reason("Strong match")
                        .build()));

        DiscoverResponse response = aiService.discover(QUERY);

        assertThat(response.getQuery()).isEqualTo(QUERY);
        assertThat(response.getCount()).isEqualTo(1);
        assertThat(response.getResults()).hasSize(1);
        assertThat(response.getResults().get(0).getProfileId()).isEqualTo(PROFILE_ID);
        assertThat(response.getResults().get(0).getScore()).isEqualTo(0.95);
    }

    @Test
    void status_reportsServiceUp() {
        StatusResponse status = aiService.status();

        assertThat(status.service()).isEqualTo("ai-service");
        assertThat(status.status()).isEqualTo("UP");
    }
}
