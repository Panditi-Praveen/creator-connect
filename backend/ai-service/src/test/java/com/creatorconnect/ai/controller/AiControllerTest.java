package com.creatorconnect.ai.controller;

import com.creatorconnect.ai.config.SecurityBeansConfig;
import com.creatorconnect.ai.dto.response.DiscoverResponse;
import com.creatorconnect.ai.dto.response.StatusResponse;
import com.creatorconnect.ai.dto.response.TalentResult;
import com.creatorconnect.ai.exception.AiConfigurationException;
import com.creatorconnect.ai.exception.AiLlmException;
import com.creatorconnect.ai.exception.ProfileServiceUnavailableException;
import com.creatorconnect.ai.security.JwtAuthenticationEntryPoint;
import com.creatorconnect.ai.security.JwtAuthenticationFilter;
import com.creatorconnect.ai.security.JwtService;
import com.creatorconnect.ai.service.AiService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web-layer tests for {@link AiController}.
 *
 * <p>{@code @WebMvcTest} loads only the controller slice; the security chain,
 * the JWT filter and the 401 entry point are recreated in a small test
 * configuration so the tests exercise the real bearer-token path (a mocked
 * {@link JwtService} makes any {@code Bearer <anything>} header authenticate
 * unless overridden per test). The global exception handler (a
 * {@code @ControllerAdvice}) is picked up automatically by the slice, so the
 * error-contract mappings (400/401/502/503) are asserted against the real
 * handler.
 */
@WebMvcTest(
        value = AiController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class)
)
@Import({SecurityBeansConfig.class, AiControllerTest.SecurityTestConfig.class})
class AiControllerTest {

    private static final UUID USER_ID = UUID.fromString("7b092f57-a53d-46dd-b2e0-4c8f0289fb91");
    private static final UUID PROFILE_ID = UUID.fromString("8dade014-d0c9-470d-acb8-744a3712fc76");

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AiService aiService;

    @MockBean
    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        when(jwtService.isValid(anyString())).thenReturn(true);
        when(jwtService.extractUserId(anyString())).thenReturn(USER_ID);
        when(jwtService.extractUsername(anyString())).thenReturn("praveen@gmail.com");
        when(jwtService.extractRole(anyString())).thenReturn("CREATOR");
    }

    @Test
    void statusEndpoint_isPublicAndReportsUp() throws Exception {
        when(aiService.status()).thenReturn(new StatusResponse("ai-service", "UP"));

        mockMvc.perform(get("/ai/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("AI service is up"))
                .andExpect(jsonPath("$.data.service").value("ai-service"))
                .andExpect(jsonPath("$.data.status").value("UP"));
    }

    @Test
    void discover_withoutJwt_returns401() throws Exception {
        mockMvc.perform(post("/ai/discover")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":\"find a designer\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Authentication required"));
    }

    @Test
    void discover_withInvalidJwt_returns401() throws Exception {
        when(jwtService.isValid("not-a-jwt")).thenReturn(false);

        mockMvc.perform(post("/ai/discover")
                        .header("Authorization", "Bearer not-a-jwt")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":\"find a designer\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Authentication required"));
    }

    @Test
    void discover_withValidJwt_returnsRankedResultsPerContract() throws Exception {
        TalentResult result = TalentResult.builder()
                .profileId(PROFILE_ID)
                .userId(USER_ID)
                .name("Alex Rivera")
                .headline("Senior Video Editor - Short-form & Travel Content")
                .skills("Video Editing, After Effects")
                .location("Mumbai, India")
                .availableForHire(true)
                .score(0.95)
                .reason("Strong match for short-form travel video editing")
                .build();
        when(aiService.discover("find a video editor"))
                .thenReturn(DiscoverResponse.builder()
                        .query("find a video editor")
                        .count(1)
                        .results(List.of(result))
                        .build());

        mockMvc.perform(post("/ai/discover")
                        .header("Authorization", "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":\"find a video editor\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("Talent discovery completed"))
                .andExpect(jsonPath("$.data.query").value("find a video editor"))
                .andExpect(jsonPath("$.data.count").value(1))
                .andExpect(jsonPath("$.data.results[0].profileId").value(PROFILE_ID.toString()))
                .andExpect(jsonPath("$.data.results[0].name").value("Alex Rivera"))
                .andExpect(jsonPath("$.data.results[0].score").value(0.95))
                .andExpect(jsonPath("$.data.results[0].reason").value("Strong match for short-form travel video editing"));
    }

    @Test
    void discover_withBlankQuery_returns400() throws Exception {
        mockMvc.perform(post("/ai/discover")
                        .header("Authorization", "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("query Query is required"));
    }

    @Test
    void discover_withOversizedQuery_returns400() throws Exception {
        String oversized = "x".repeat(501);
        mockMvc.perform(post("/ai/discover")
                        .header("Authorization", "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":\"" + oversized + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("query Query must not exceed 500 characters"));
    }

    @Test
    void discover_whenLlmFails_returns502() throws Exception {
        when(aiService.discover(anyString()))
                .thenThrow(new AiLlmException("AI service is temporarily unavailable", null));

        mockMvc.perform(post("/ai/discover")
                        .header("Authorization", "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":\"find a video editor\"}"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.status").value(502))
                .andExpect(jsonPath("$.message").value("AI service is temporarily unavailable"));
    }

    @Test
    void discover_whenAiNotConfigured_returns503() throws Exception {
        when(aiService.discover(anyString()))
                .thenThrow(new AiConfigurationException(
                        "AI service is not configured: set the OPENAI_API_KEY environment variable"));

        mockMvc.perform(post("/ai/discover")
                        .header("Authorization", "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":\"find a video editor\"}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.message").value(
                        "AI service is not configured: set the OPENAI_API_KEY environment variable"));
    }

    @Test
    void discover_whenProfileServiceDown_returns503() throws Exception {
        when(aiService.discover(anyString()))
                .thenThrow(new ProfileServiceUnavailableException("The Profile Service is temporarily unavailable"));

        mockMvc.perform(post("/ai/discover")
                        .header("Authorization", "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":\"find a video editor\"}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.message").value("The Profile Service is temporarily unavailable"));
    }

    @Test
    void unknownPath_withTrailingSlash_returns404() throws Exception {
        mockMvc.perform(get("/ai/").header("Authorization", "Bearer valid-token"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Resource not found"));
    }

    /**
     * Supplies only the two security beans the production
     * {@link SecurityBeansConfig} depends on: the real JWT filter (backed by
     * the mocked {@link JwtService}) and the real 401 entry point. The filter
     * chain itself is the production one — imported via
     * {@code @Import(SecurityBeansConfig.class)} — so the tests exercise the
     * exact same authorization rules as production.
     */
    @TestConfiguration
    static class SecurityTestConfig {

        @Bean
        JwtAuthenticationFilter jwtAuthenticationFilter(JwtService jwtService) {
            return new JwtAuthenticationFilter(jwtService);
        }

        @Bean
        JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint(ObjectMapper objectMapper) {
            return new JwtAuthenticationEntryPoint(objectMapper);
        }
    }
}
