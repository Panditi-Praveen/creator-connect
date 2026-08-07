package com.creatorconnect.hiring.controller;

import com.creatorconnect.hiring.config.SecurityBeansConfig;
import com.creatorconnect.hiring.dto.response.ApplicationResponse;
import com.creatorconnect.hiring.entity.ApplicationStatus;
import com.creatorconnect.hiring.exception.ApplicationAccessDeniedException;
import com.creatorconnect.hiring.exception.ApplicationNotFoundException;
import com.creatorconnect.hiring.exception.ApplicationStatusConflictException;
import com.creatorconnect.hiring.exception.ApplicationValidationException;
import com.creatorconnect.hiring.exception.DuplicateApplicationException;
import com.creatorconnect.hiring.security.JwtAuthenticationEntryPoint;
import com.creatorconnect.hiring.security.JwtAuthenticationFilter;
import com.creatorconnect.hiring.security.JwtService;
import com.creatorconnect.hiring.service.ApplicationService;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web-layer tests for {@link ApplicationController}.
 *
 * <p>{@code @WebMvcTest} loads only the controller slice; the security chain,
 * the JWT filter and the 401 entry point are recreated in a small test
 * configuration so the tests exercise the real bearer-token path (a mocked
 * {@link JwtService} makes any {@code Bearer <anything>} header authenticate
 * as {@link #FREELANCER_ID} with the {@code FREELANCER} role by default —
 * individual tests re-stub the role where a creator is needed). The global
 * exception handler (a {@code @ControllerAdvice}) is picked up automatically
 * by the slice.
 */
@WebMvcTest(
        value = ApplicationController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class)
)
@Import({SecurityBeansConfig.class, ApplicationControllerTest.SecurityTestConfig.class})
class ApplicationControllerTest {

    private static final UUID FREELANCER_ID = UUID.fromString("7b092f57-a53d-46dd-b2e0-4c8f0289fb91");
    private static final UUID PROJECT_ID = UUID.fromString("9d2b4f79-c75f-68ff-d402-6ea024a1bd13");
    private static final UUID APPLICATION_ID = UUID.fromString("6a3c2e18-d46a-4f8b-9e0c-1b2d3e4f5a6b");

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ApplicationService applicationService;

    @MockBean
    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        when(jwtService.isValid(anyString())).thenReturn(true);
        when(jwtService.extractUserId(anyString())).thenReturn(FREELANCER_ID);
        when(jwtService.extractUsername(anyString())).thenReturn("freelancer@gmail.com");
        when(jwtService.extractRole(anyString())).thenReturn("FREELANCER");
    }

    @Test
    void apply_withValidTokenAndPayload_returns201() throws Exception {
        when(applicationService.apply(eq(FREELANCER_ID), eq("FREELANCER"), any()))
                .thenReturn(applicationResponse(ApplicationStatus.PENDING));

        mockMvc.perform(post("/applications")
                        .header("Authorization", "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validApplyPayload()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.message").value("Application submitted successfully"))
                .andExpect(jsonPath("$.data.projectId").value(PROJECT_ID.toString()))
                .andExpect(jsonPath("$.data.freelancerId").value(FREELANCER_ID.toString()))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.path").value("/applications"));
    }

    @Test
    void apply_withMissingToken_returns401() throws Exception {
        mockMvc.perform(post("/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validApplyPayload()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Authentication required"));
    }

    @Test
    void apply_withInvalidPayload_returns400() throws Exception {
        mockMvc.perform(post("/applications")
                        .header("Authorization", "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void apply_withNegativeBudget_returns400() throws Exception {
        String payload = """
                {
                  "projectId": "%s",
                  "proposal": "I can deliver this.",
                  "expectedBudget": -10.00,
                  "estimatedDuration": "2 weeks"
                }
                """.formatted(PROJECT_ID);

        mockMvc.perform(post("/applications")
                        .header("Authorization", "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void apply_byCreator_returns403() throws Exception {
        when(jwtService.extractRole(anyString())).thenReturn("CREATOR");
        when(applicationService.apply(eq(FREELANCER_ID), eq("CREATOR"), any()))
                .thenThrow(new ApplicationAccessDeniedException("Only freelancers can apply to projects"));

        mockMvc.perform(post("/applications")
                        .header("Authorization", "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validApplyPayload()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void apply_duplicate_returns409() throws Exception {
        when(applicationService.apply(eq(FREELANCER_ID), eq("FREELANCER"), any()))
                .thenThrow(new DuplicateApplicationException("You have already applied to this project"));

        mockMvc.perform(post("/applications")
                        .header("Authorization", "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validApplyPayload()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void getMyApplications_returnsPage() throws Exception {
        when(applicationService.getMyApplications(eq(FREELANCER_ID), any()))
                .thenReturn(new PageImpl<>(List.of(applicationResponse(ApplicationStatus.PENDING))));

        mockMvc.perform(get("/applications/my")
                        .param("page", "0")
                        .param("size", "20")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].id").value(APPLICATION_ID.toString()))
                .andExpect(jsonPath("$.data.content[0].status").value("PENDING"))
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    void getApplicationsForProject_byCreator_returns200() throws Exception {
        when(jwtService.extractRole(anyString())).thenReturn("CREATOR");
        when(applicationService.getApplicationsForProject(eq("CREATOR"), eq(PROJECT_ID), any()))
                .thenReturn(new PageImpl<>(List.of(applicationResponse(ApplicationStatus.PENDING))));

        mockMvc.perform(get("/applications/project/{projectId}", PROJECT_ID)
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].projectId").value(PROJECT_ID.toString()));
    }

    @Test
    void getApplicationsForProject_byFreelancer_returns403() throws Exception {
        when(applicationService.getApplicationsForProject(eq("FREELANCER"), eq(PROJECT_ID), any()))
                .thenThrow(new ApplicationAccessDeniedException("Only creators can view applications for a project"));

        mockMvc.perform(get("/applications/project/{projectId}", PROJECT_ID)
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void getApplicationsForProject_withMalformedUuid_returns400() throws Exception {
        mockMvc.perform(get("/applications/project/not-a-uuid")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void updateStatus_byCreator_returns200() throws Exception {
        when(jwtService.extractRole(anyString())).thenReturn("CREATOR");
        when(applicationService.updateStatus(eq("CREATOR"), eq(APPLICATION_ID), any()))
                .thenReturn(applicationResponse(ApplicationStatus.ACCEPTED));

        mockMvc.perform(put("/applications/{id}/status", APPLICATION_ID)
                        .header("Authorization", "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"ACCEPTED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Application status updated successfully"))
                .andExpect(jsonPath("$.data.status").value("ACCEPTED"));
    }

    @Test
    void updateStatus_byFreelancer_returns403() throws Exception {
        when(applicationService.updateStatus(eq("FREELANCER"), eq(APPLICATION_ID), any()))
                .thenThrow(new ApplicationAccessDeniedException("Only creators can update application status"));

        mockMvc.perform(put("/applications/{id}/status", APPLICATION_ID)
                        .header("Authorization", "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"ACCEPTED\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void updateStatus_withIllegalDecision_returns400() throws Exception {
        when(jwtService.extractRole(anyString())).thenReturn("CREATOR");
        when(applicationService.updateStatus(eq("CREATOR"), eq(APPLICATION_ID), any()))
                .thenThrow(new ApplicationValidationException("Status must be ACCEPTED or REJECTED"));

        mockMvc.perform(put("/applications/{id}/status", APPLICATION_ID)
                        .header("Authorization", "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"PENDING\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void updateStatus_whenMissing_returns404() throws Exception {
        when(jwtService.extractRole(anyString())).thenReturn("CREATOR");
        when(applicationService.updateStatus(eq("CREATOR"), eq(APPLICATION_ID), any()))
                .thenThrow(new ApplicationNotFoundException("Application not found: " + APPLICATION_ID));

        mockMvc.perform(put("/applications/{id}/status", APPLICATION_ID)
                        .header("Authorization", "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"ACCEPTED\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void updateStatus_whenNotPending_returns409() throws Exception {
        when(jwtService.extractRole(anyString())).thenReturn("CREATOR");
        when(applicationService.updateStatus(eq("CREATOR"), eq(APPLICATION_ID), any()))
                .thenThrow(new ApplicationStatusConflictException(
                        "Only pending applications can be decided on (current status: REJECTED)"));

        mockMvc.perform(put("/applications/{id}/status", APPLICATION_ID)
                        .header("Authorization", "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"ACCEPTED\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void withdraw_byOwner_returns200() throws Exception {
        mockMvc.perform(delete("/applications/{id}", APPLICATION_ID)
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Application withdrawn successfully"))
                .andExpect(jsonPath("$.data").doesNotExist());

        verify(applicationService).withdraw(FREELANCER_ID, APPLICATION_ID);
    }

    @Test
    void withdraw_byNonOwner_returns403() throws Exception {
        doThrow(new ApplicationAccessDeniedException("You can only withdraw your own applications"))
                .when(applicationService).withdraw(FREELANCER_ID, APPLICATION_ID);

        mockMvc.perform(delete("/applications/{id}", APPLICATION_ID)
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void withdraw_whenMissing_returns404() throws Exception {
        doThrow(new ApplicationNotFoundException("Application not found: " + APPLICATION_ID))
                .when(applicationService).withdraw(FREELANCER_ID, APPLICATION_ID);

        mockMvc.perform(delete("/applications/{id}", APPLICATION_ID)
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void withdraw_withMalformedUuid_returns400() throws Exception {
        mockMvc.perform(delete("/applications/not-a-uuid")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    private String validApplyPayload() {
        return """
                {
                  "projectId": "%s",
                  "proposal": "I can deliver this in 2 weeks.",
                  "expectedBudget": 500.00,
                  "estimatedDuration": "2 weeks"
                }
                """.formatted(PROJECT_ID);
    }

    private ApplicationResponse applicationResponse(ApplicationStatus status) {
        return ApplicationResponse.builder()
                .id(APPLICATION_ID)
                .projectId(PROJECT_ID)
                .freelancerId(FREELANCER_ID)
                .proposal("I can deliver this in 2 weeks.")
                .expectedBudget(new BigDecimal("500.00"))
                .estimatedDuration("2 weeks")
                .status(status)
                .build();
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
