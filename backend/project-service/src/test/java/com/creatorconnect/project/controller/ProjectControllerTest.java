package com.creatorconnect.project.controller;

import com.creatorconnect.project.config.SecurityBeansConfig;
import com.creatorconnect.project.dto.request.ProjectFilter;
import com.creatorconnect.project.dto.response.ProjectResponse;
import com.creatorconnect.project.entity.ProjectStatus;
import com.creatorconnect.project.exception.ProjectAccessDeniedException;
import com.creatorconnect.project.exception.ProjectNotFoundException;
import com.creatorconnect.project.security.JwtAuthenticationEntryPoint;
import com.creatorconnect.project.security.JwtAuthenticationFilter;
import com.creatorconnect.project.security.JwtService;
import com.creatorconnect.project.service.ProjectService;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.mockito.ArgumentCaptor;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web-layer tests for {@link ProjectController}.
 *
 * <p>{@code @WebMvcTest} loads only the controller slice; the security chain,
 * the JWT filter and the 401 entry point are recreated in a small test
 * configuration so the tests exercise the real bearer-token path (a mocked
 * {@link JwtService} makes any {@code Bearer <anything>} header authenticate as
 * {@link #OWNER_ID}). The global exception handler (a {@code @ControllerAdvice})
 * is picked up automatically by the slice.
 */
@WebMvcTest(
        value = ProjectController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class)
)
@Import({SecurityBeansConfig.class, ProjectControllerTest.SecurityTestConfig.class})
class ProjectControllerTest {

    private static final UUID OWNER_ID = UUID.fromString("7b092f57-a53d-46dd-b2e0-4c8f0289fb91");
    private static final UUID PROJECT_ID = UUID.fromString("9d2b4f79-c75f-68ff-d402-6ea024a1bd13");

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProjectService projectService;

    @MockBean
    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        when(jwtService.isValid(anyString())).thenReturn(true);
        when(jwtService.extractUserId(anyString())).thenReturn(OWNER_ID);
        when(jwtService.extractUsername(anyString())).thenReturn("praveen@gmail.com");
        when(jwtService.extractRole(anyString())).thenReturn("CREATOR");
    }

    @Test
    void createProject_withValidTokenAndPayload_returns201() throws Exception {
        when(projectService.createProject(eq(OWNER_ID), any())).thenReturn(projectResponse());

        mockMvc.perform(post("/projects")
                        .header("Authorization", "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCreatePayload()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.message").value("Project created successfully"))
                .andExpect(jsonPath("$.data.title").value("YouTube Intro Package"))
                .andExpect(jsonPath("$.data.status").value("OPEN"))
                .andExpect(jsonPath("$.path").value("/projects"));
    }

    @Test
    void createProject_withMissingToken_returns401() throws Exception {
        mockMvc.perform(post("/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCreatePayload()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Authentication required"));
    }

    @Test
    void createProject_withInvalidPayload_returns400() throws Exception {
        mockMvc.perform(post("/projects")
                        .header("Authorization", "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void getAllProjects_returnsList() throws Exception {
        when(projectService.getAllProjects(any())).thenReturn(List.of(projectResponse()));

        mockMvc.perform(get("/projects").header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(PROJECT_ID.toString()))
                .andExpect(jsonPath("$.data[0].title").value("YouTube Intro Package"));
    }

    @Test
    void getAllProjects_withFilters_passesFilterToService() throws Exception {
        mockMvc.perform(get("/projects")
                        .param("category", "Video Editing")
                        .param("skill", "After Effects")
                        .param("budgetMin", "100")
                        .param("budgetMax", "1000")
                        .param("keyword", "youtube")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk());

        ArgumentCaptor<ProjectFilter> captor = ArgumentCaptor.forClass(ProjectFilter.class);
        verify(projectService).getAllProjects(captor.capture());
        ProjectFilter filter = captor.getValue();
        assertThat(filter.getCategory()).isEqualTo("Video Editing");
        assertThat(filter.getSkill()).isEqualTo("After Effects");
        assertThat(filter.getBudgetMin()).isEqualByComparingTo(new BigDecimal("100"));
        assertThat(filter.getBudgetMax()).isEqualByComparingTo(new BigDecimal("1000"));
        assertThat(filter.getKeyword()).isEqualTo("youtube");
        assertThat(filter.getExperienceLevel()).isNull();
        assertThat(filter.getLocation()).isNull();
    }

    @Test
    void getAllProjects_withInvalidBudgetFilter_returns400() throws Exception {
        mockMvc.perform(get("/projects")
                        .param("budgetMin", "not-a-number")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void getAllProjects_withNegativeBudgetFilter_returns400() throws Exception {
        mockMvc.perform(get("/projects")
                        .param("budgetMin", "-50")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void getMyProjects_usesPrincipalUserId() throws Exception {
        when(projectService.getProjectsByUserId(eq(OWNER_ID), any())).thenReturn(List.of(projectResponse()));

        mockMvc.perform(get("/projects/my").header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].userId").value(OWNER_ID.toString()));

        verify(projectService).getProjectsByUserId(eq(OWNER_ID), any());
    }

    @Test
    void getMyProjects_withFilters_passesFilterToService() throws Exception {
        mockMvc.perform(get("/projects/my")
                        .param("experienceLevel", "Intermediate")
                        .param("location", "Remote")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk());

        ArgumentCaptor<ProjectFilter> captor = ArgumentCaptor.forClass(ProjectFilter.class);
        verify(projectService).getProjectsByUserId(eq(OWNER_ID), captor.capture());
        assertThat(captor.getValue().getExperienceLevel()).isEqualTo("Intermediate");
        assertThat(captor.getValue().getLocation()).isEqualTo("Remote");
    }

    @Test
    void getProjectById_returnsProject() throws Exception {
        when(projectService.getProjectById(PROJECT_ID)).thenReturn(projectResponse());

        mockMvc.perform(get("/projects/{id}", PROJECT_ID).header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(PROJECT_ID.toString()));
    }

    @Test
    void getProjectById_whenMissing_returns404() throws Exception {
        when(projectService.getProjectById(PROJECT_ID))
                .thenThrow(new ProjectNotFoundException("Project not found: " + PROJECT_ID));

        mockMvc.perform(get("/projects/{id}", PROJECT_ID).header("Authorization", "Bearer valid-token"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void updateProject_byOwner_returns200() throws Exception {
        when(projectService.updateProject(eq(OWNER_ID), eq(PROJECT_ID), any())).thenReturn(projectResponse());

        mockMvc.perform(put("/projects/{id}", PROJECT_ID)
                        .header("Authorization", "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"IN_PROGRESS\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Project updated successfully"));
    }

    @Test
    void updateProject_byNonOwner_returns403() throws Exception {
        when(projectService.updateProject(eq(OWNER_ID), eq(PROJECT_ID), any()))
                .thenThrow(new ProjectAccessDeniedException("You do not have permission to modify this project"));

        mockMvc.perform(put("/projects/{id}", PROJECT_ID)
                        .header("Authorization", "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"IN_PROGRESS\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void deleteProject_byOwner_returns200() throws Exception {
        mockMvc.perform(delete("/projects/{id}", PROJECT_ID)
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Project deleted successfully"))
                .andExpect(jsonPath("$.data").doesNotExist());

        verify(projectService).deleteProject(OWNER_ID, PROJECT_ID);
    }

    @Test
    void deleteProject_whenMissing_returns404() throws Exception {
        org.mockito.Mockito.doThrow(new ProjectNotFoundException("Project not found: " + PROJECT_ID))
                .when(projectService).deleteProject(OWNER_ID, PROJECT_ID);

        mockMvc.perform(delete("/projects/{id}", PROJECT_ID)
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getProjectById_withMalformedUuid_returns400() throws Exception {
        mockMvc.perform(get("/projects/not-a-uuid").header("Authorization", "Bearer valid-token"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void createProject_withPastDeadline_returns400() throws Exception {
        String payload = """
                {
                  "title": "Expired deadline project",
                  "description": "Should be rejected.",
                  "category": "Video Editing",
                  "budget": 100.00,
                  "duration": "1 week",
                  "experienceLevel": "Beginner",
                  "applicationDeadline": "2020-01-01"
                }
                """;

        mockMvc.perform(post("/projects")
                        .header("Authorization", "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void createProject_withBlankSkill_returns400() throws Exception {
        String payload = """
                {
                  "title": "Blank skill project",
                  "description": "Should be rejected.",
                  "category": "Video Editing",
                  "skillsRequired": [""],
                  "budget": 100.00,
                  "duration": "1 week",
                  "experienceLevel": "Beginner",
                  "applicationDeadline": "%s"
                }
                """.formatted(LocalDate.now().plusDays(30));

        mockMvc.perform(post("/projects")
                        .header("Authorization", "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    private String validCreatePayload() {
        // The deadline is computed at runtime so the @Future constraint on
        // ProjectRequest can never make this test fail as time moves forward.
        return String.format("""
                {
                  "title": "YouTube Intro Package",
                  "description": "Need a 15-second animated intro for a new YouTube channel.",
                  "category": "Video Editing",
                  "skillsRequired": ["After Effects", "Motion Design"],
                  "budget": 500.00,
                  "duration": "1 week",
                  "experienceLevel": "Intermediate",
                  "location": "Remote",
                  "applicationDeadline": "%s"
                }
                """, LocalDate.now().plusDays(30));
    }

    private ProjectResponse projectResponse() {
        return ProjectResponse.builder()
                .id(PROJECT_ID)
                .userId(OWNER_ID)
                .title("YouTube Intro Package")
                .description("Need a 15-second animated intro for a new YouTube channel.")
                .category("Video Editing")
                .skillsRequired(List.of("After Effects", "Motion Design"))
                .budget(new BigDecimal("500.00"))
                .duration("1 week")
                .experienceLevel("Intermediate")
                .location("Remote")
                .status(ProjectStatus.OPEN)
                .applicationDeadline(LocalDate.of(2026, 9, 30))
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
