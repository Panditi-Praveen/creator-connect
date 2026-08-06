package com.creatorconnect.project.service.impl;

import com.creatorconnect.project.dto.request.ProjectFilter;
import com.creatorconnect.project.dto.request.ProjectRequest;
import com.creatorconnect.project.dto.request.UpdateProjectRequest;
import com.creatorconnect.project.dto.response.ProjectResponse;
import com.creatorconnect.project.entity.Project;
import com.creatorconnect.project.entity.ProjectStatus;
import com.creatorconnect.project.exception.ProjectAccessDeniedException;
import com.creatorconnect.project.exception.ProjectNotFoundException;
import com.creatorconnect.project.mapper.ProjectMapper;
import com.creatorconnect.project.repository.ProjectRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ProjectServiceImpl} — create/get/update/delete flows
 * plus the ownership rules, using mocked collaborators.
 */
@ExtendWith(MockitoExtension.class)
class ProjectServiceImplTest {

    private static final UUID OWNER_ID = UUID.fromString("7b092f57-a53d-46dd-b2e0-4c8f0289fb91");
    private static final UUID OTHER_USER_ID = UUID.fromString("8c1a3e68-b64e-57ee-c3f1-5d9f1390ac02");
    private static final UUID PROJECT_ID = UUID.fromString("9d2b4f79-c75f-68ff-d402-6ea024a1bd13");

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private ProjectMapper projectMapper;

    @InjectMocks
    private ProjectServiceImpl projectService;

    @Test
    void createProject_persistsAndReturnsResponse() {
        ProjectRequest request = projectRequest();
        Project entity = project(OWNER_ID);
        when(projectMapper.toEntity(OWNER_ID, request)).thenReturn(entity);
        when(projectRepository.save(entity)).thenReturn(entity);
        when(projectMapper.toResponse(entity)).thenReturn(projectResponse(OWNER_ID));

        ProjectResponse response = projectService.createProject(OWNER_ID, request);

        assertThat(response.getUserId()).isEqualTo(OWNER_ID);
        assertThat(response.getTitle()).isEqualTo("YouTube Intro Package");
        verify(projectRepository).save(entity);
    }

    @Test
    void getAllProjects_returnsMappedList() {
        Project entity = project(OWNER_ID);
        when(projectRepository.findAllByFilters(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of(entity));
        when(projectMapper.toResponse(entity)).thenReturn(projectResponse(OWNER_ID));

        List<ProjectResponse> responses = projectService.getAllProjects(ProjectFilter.builder().build());

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getId()).isEqualTo(PROJECT_ID);
    }

    @Test
    void getAllProjects_forwardsFilterToRepository() {
        ProjectFilter filter = ProjectFilter.builder()
                .category("Video Editing")
                .skill("After Effects")
                .budgetMin(new BigDecimal("100"))
                .keyword("youtube")
                .build();
        when(projectRepository.findAllByFilters(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of());

        projectService.getAllProjects(filter);

        verify(projectRepository).findAllByFilters(
                "Video Editing", "After Effects", new BigDecimal("100"), null, null, null, "youtube");
    }

    @Test
    void getAllProjects_withBlankFilterValues_forwardsNulls() {
        ProjectFilter filter = ProjectFilter.builder()
                .category("")
                .skill("   ")
                .location("")
                .keyword("\t")
                .build();
        when(projectRepository.findAllByFilters(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of());

        projectService.getAllProjects(filter);

        // Empty / whitespace-only query parameters mean "no filter" — they
        // must be forwarded as null, never as an active filter matching
        // nothing.
        verify(projectRepository).findAllByFilters(
                null, null, null, null, null, null, null);
    }

    @Test
    void getAllProjects_withWhitespacePaddedValues_trimsThem() {
        ProjectFilter filter = ProjectFilter.builder()
                .category("  Video Editing  ")
                .experienceLevel(" Intermediate ")
                .build();
        when(projectRepository.findAllByFilters(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of());

        projectService.getAllProjects(filter);

        verify(projectRepository).findAllByFilters(
                "Video Editing", null, null, null, "Intermediate", null, null);
    }

    @Test
    void getProjectById_whenFound_returnsResponse() {
        Project entity = project(OWNER_ID);
        when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(entity));
        when(projectMapper.toResponse(entity)).thenReturn(projectResponse(OWNER_ID));

        ProjectResponse response = projectService.getProjectById(PROJECT_ID);

        assertThat(response.getId()).isEqualTo(PROJECT_ID);
    }

    @Test
    void getProjectById_whenMissing_throwsProjectNotFound() {
        when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.getProjectById(PROJECT_ID))
                .isInstanceOf(ProjectNotFoundException.class);
    }

    @Test
    void getProjectsByUserId_returnsOwnedProjects() {
        Project entity = project(OWNER_ID);
        when(projectRepository.findByUserIdAndFilters(eq(OWNER_ID), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of(entity));
        when(projectMapper.toResponse(entity)).thenReturn(projectResponse(OWNER_ID));

        List<ProjectResponse> responses =
                projectService.getProjectsByUserId(OWNER_ID, ProjectFilter.builder().build());

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getUserId()).isEqualTo(OWNER_ID);
    }

    @Test
    void getProjectsByUserId_forwardsFilterToRepository() {
        ProjectFilter filter = ProjectFilter.builder()
                .category("Graphic Design")
                .budgetMax(new BigDecimal("500"))
                .build();
        when(projectRepository.findByUserIdAndFilters(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of());

        projectService.getProjectsByUserId(OWNER_ID, filter);

        verify(projectRepository).findByUserIdAndFilters(
                OWNER_ID, "Graphic Design", null, null, new BigDecimal("500"), null, null, null);
    }

    @Test
    void updateProject_byOwner_updatesAndReturnsResponse() {
        Project entity = project(OWNER_ID);
        UpdateProjectRequest request = updateProjectRequest();
        when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(entity));
        when(projectRepository.save(entity)).thenReturn(entity);
        when(projectMapper.toResponse(entity)).thenReturn(projectResponse(OWNER_ID));

        ProjectResponse response = projectService.updateProject(OWNER_ID, PROJECT_ID, request);

        assertThat(response.getId()).isEqualTo(PROJECT_ID);
        verify(projectMapper).applyUpdate(entity, request);
    }

    @Test
    void updateProject_byNonOwner_throwsAccessDenied() {
        Project entity = project(OWNER_ID);
        when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> projectService.updateProject(OTHER_USER_ID, PROJECT_ID, updateProjectRequest()))
                .isInstanceOf(ProjectAccessDeniedException.class);
        verify(projectRepository, never()).save(any(Project.class));
    }

    @Test
    void updateProject_whenMissing_throwsProjectNotFound() {
        when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.updateProject(OWNER_ID, PROJECT_ID, updateProjectRequest()))
                .isInstanceOf(ProjectNotFoundException.class);
        verify(projectRepository, never()).save(any(Project.class));
    }

    @Test
    void deleteProject_byOwner_deletes() {
        Project entity = project(OWNER_ID);
        when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(entity));

        projectService.deleteProject(OWNER_ID, PROJECT_ID);

        verify(projectRepository).delete(entity);
    }

    @Test
    void deleteProject_byNonOwner_throwsAccessDenied() {
        Project entity = project(OWNER_ID);
        when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> projectService.deleteProject(OTHER_USER_ID, PROJECT_ID))
                .isInstanceOf(ProjectAccessDeniedException.class);
        verify(projectRepository, never()).delete(any(Project.class));
    }

    @Test
    void deleteProject_whenMissing_throwsProjectNotFound() {
        when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.deleteProject(OWNER_ID, PROJECT_ID))
                .isInstanceOf(ProjectNotFoundException.class);
        verify(projectRepository, never()).delete(any(Project.class));
    }

    private ProjectRequest projectRequest() {
        return ProjectRequest.builder()
                .title("YouTube Intro Package")
                .description("Need a 15-second animated intro for a new YouTube channel.")
                .category("Video Editing")
                .skillsRequired(List.of("After Effects", "Motion Design"))
                .budget(new BigDecimal("500.00"))
                .duration("1 week")
                .experienceLevel("Intermediate")
                .location("Remote")
                .applicationDeadline(LocalDate.now().plusDays(30))
                .build();
    }

    private UpdateProjectRequest updateProjectRequest() {
        return UpdateProjectRequest.builder()
                .status(ProjectStatus.IN_PROGRESS)
                .budget(new BigDecimal("750.00"))
                .build();
    }

    private Project project(UUID userId) {
        return Project.builder()
                .id(PROJECT_ID)
                .userId(userId)
                .title("YouTube Intro Package")
                .description("Need a 15-second animated intro for a new YouTube channel.")
                .category("Video Editing")
                .skillsRequired(List.of("After Effects", "Motion Design"))
                .budget(new BigDecimal("500.00"))
                .duration("1 week")
                .experienceLevel("Intermediate")
                .status(ProjectStatus.OPEN)
                .applicationDeadline(LocalDate.now().plusDays(30))
                .build();
    }

    private ProjectResponse projectResponse(UUID userId) {
        return ProjectResponse.builder()
                .id(PROJECT_ID)
                .userId(userId)
                .title("YouTube Intro Package")
                .category("Video Editing")
                .status(ProjectStatus.OPEN)
                .build();
    }
}
