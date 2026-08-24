package com.creatorconnect.profile.controller;

import com.creatorconnect.profile.config.SecurityBeansConfig;
import com.creatorconnect.profile.dto.response.ProfileResponse;
import com.creatorconnect.profile.repository.ProfileRepository;
import com.creatorconnect.profile.security.JwtAuthenticationEntryPoint;
import com.creatorconnect.profile.security.JwtAuthenticationFilter;
import com.creatorconnect.profile.security.JwtService;
import com.creatorconnect.profile.service.ProfileService;
import com.creatorconnect.profile.service.impl.FileStorageService;
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
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web-layer tests for {@link ProfileController}.
 *
 * <p>{@code @WebMvcTest} loads only the controller slice; the security chain,
 * the JWT filter and the 401 entry point are recreated in a small test
 * configuration so the tests exercise the real bearer-token path (a mocked
 * {@link JwtService} makes any {@code Bearer <anything>} header authenticate).
 * The global exception handler (a {@code @ControllerAdvice}) is picked up
 * automatically by the slice.
 */
@WebMvcTest(
        value = ProfileController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class)
)
@Import({SecurityBeansConfig.class, ProfileControllerTest.SecurityTestConfig.class})
class ProfileControllerTest {

    private static final UUID USER_ID = UUID.fromString("7b092f57-a53d-46dd-b2e0-4c8f0289fb91");

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProfileService profileService;

    @MockBean
    private FileStorageService fileStorageService;

    @MockBean
    private ProfileRepository profileRepository;

    @MockBean
    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        when(jwtService.isValid(anyString())).thenReturn(true);
        when(jwtService.extractUserId(anyString())).thenReturn(USER_ID);
        when(jwtService.extractUsername(anyString())).thenReturn("praveen@gmail.com");
        when(jwtService.extractRole(anyString())).thenReturn("FREELANCER");
    }

    @Test
    void listProfiles_returnsAllProfiles() throws Exception {
        ProfileResponse profile = ProfileResponse.builder()
                .id(UUID.randomUUID())
                .userId(USER_ID)
                .firstName("Praveen")
                .lastName("Panditi")
                .skills("Java, Spring Boot")
                .build();
        when(profileService.getFreelancerProfiles()).thenReturn(List.of(profile));

        mockMvc.perform(get("/profile/freelancers").header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("Profiles retrieved successfully"))
                .andExpect(jsonPath("$.data[0].firstName").value("Praveen"))
                .andExpect(jsonPath("$.data[0].skills").value("Java, Spring Boot"));
    }

    @Test
    void unknownPath_withTrailingSlash_returns404() throws Exception {
        mockMvc.perform(get("/profile/").header("Authorization", "Bearer valid-token"))
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
