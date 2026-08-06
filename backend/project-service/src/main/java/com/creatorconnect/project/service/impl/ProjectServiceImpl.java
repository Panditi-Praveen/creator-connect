package com.creatorconnect.project.service.impl;

import com.creatorconnect.project.dto.request.ProjectFilter;
import com.creatorconnect.project.dto.request.ProjectRequest;
import com.creatorconnect.project.dto.request.UpdateProjectRequest;
import com.creatorconnect.project.dto.response.ProjectResponse;
import com.creatorconnect.project.entity.Project;
import com.creatorconnect.project.exception.ProjectAccessDeniedException;
import com.creatorconnect.project.exception.ProjectNotFoundException;
import com.creatorconnect.project.mapper.ProjectMapper;
import com.creatorconnect.project.repository.ProjectRepository;
import com.creatorconnect.project.service.ProjectService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Concrete {@link ProjectService} implementation.
 *
 * <p>Owns the project lifecycle with these rules:
 * <ol>
 *   <li><b>Create</b> — the caller's {@code userId} (from the JWT) becomes the
 *       project owner.</li>
 *   <li><b>Get / Browse</b> — any authenticated user may view any project;
 *       missing projects yield {@link ProjectNotFoundException}.</li>
 *   <li><b>Update / Delete</b> — the caller must be the project owner; the
 *       check runs after the existence check so the API never leaks whether a
 *       project exists to non-owners ({@code 404} before {@code 403}).</li>
 * </ol>
 *
 * <p>Dependencies are injected through the constructor only (no field
 * injection). Write operations run inside one {@code @Transactional} boundary
 * so a failure rolls back cleanly.
 */
@Service
public class ProjectServiceImpl implements ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectMapper projectMapper;

    /**
     * Creates the service with its collaborators.
     *
     * @param projectRepository the project data access layer
     * @param projectMapper     the entity/DTO mapper
     */
    public ProjectServiceImpl(ProjectRepository projectRepository, ProjectMapper projectMapper) {
        this.projectRepository = projectRepository;
        this.projectMapper = projectMapper;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public ProjectResponse createProject(UUID userId, ProjectRequest request) {
        Project project = projectRepository.save(projectMapper.toEntity(userId, request));
        return projectMapper.toResponse(project);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public List<ProjectResponse> getAllProjects(ProjectFilter filter) {
        ProjectFilter normalized = normalize(filter);
        return projectRepository.findAllByFilters(
                        normalized.getCategory(),
                        normalized.getSkill(),
                        normalized.getBudgetMin(),
                        normalized.getBudgetMax(),
                        normalized.getExperienceLevel(),
                        normalized.getLocation(),
                        normalized.getKeyword()
                ).stream()
                .map(projectMapper::toResponse)
                .toList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public ProjectResponse getProjectById(UUID projectId) {
        return projectRepository.findById(projectId)
                .map(projectMapper::toResponse)
                .orElseThrow(() -> new ProjectNotFoundException("Project not found: " + projectId));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public List<ProjectResponse> getProjectsByUserId(UUID userId, ProjectFilter filter) {
        ProjectFilter normalized = normalize(filter);
        return projectRepository.findByUserIdAndFilters(
                        userId,
                        normalized.getCategory(),
                        normalized.getSkill(),
                        normalized.getBudgetMin(),
                        normalized.getBudgetMax(),
                        normalized.getExperienceLevel(),
                        normalized.getLocation(),
                        normalized.getKeyword()
                ).stream()
                .map(projectMapper::toResponse)
                .toList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public ProjectResponse updateProject(UUID authenticatedUserId, UUID projectId, UpdateProjectRequest request) {
        Project project = findOwnedProject(authenticatedUserId, projectId);
        projectMapper.applyUpdate(project, request);
        return projectMapper.toResponse(projectRepository.save(project));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void deleteProject(UUID authenticatedUserId, UUID projectId) {
        Project project = findOwnedProject(authenticatedUserId, projectId);
        projectRepository.delete(project);
    }

    /**
     * Returns a copy of the filter with blank or whitespace-only string values
     * mapped to {@code null}, so an empty query parameter (e.g.
     * {@code ?category=} sent by a frontend that cleared the filter) is
     * treated as "no filter" instead of an active filter that matches nothing.
     * Non-blank values are trimmed. Numeric fields are passed through.
     *
     * @param filter the raw filter bound from the query string (never null
     *               when Spring binds a {@code @ModelAttribute})
     * @return the normalized filter
     */
    private ProjectFilter normalize(ProjectFilter filter) {
        if (filter == null) {
            return ProjectFilter.builder().build();
        }
        return ProjectFilter.builder()
                .category(normalize(filter.getCategory()))
                .skill(normalize(filter.getSkill()))
                .budgetMin(filter.getBudgetMin())
                .budgetMax(filter.getBudgetMax())
                .experienceLevel(normalize(filter.getExperienceLevel()))
                .location(normalize(filter.getLocation()))
                .keyword(normalize(filter.getKeyword()))
                .build();
    }

    /**
     * Trims a filter value, mapping {@code null} and blank strings to
     * {@code null}.
     *
     * @param value the raw filter value
     * @return the trimmed value, or {@code null} when blank
     */
    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    /**
     * Loads the project and verifies the caller owns it.
     *
     * <p>Existence is checked first ({@code 404}) so non-owners cannot probe
     * for projects; only then is ownership enforced ({@code 403}).
     *
     * @param authenticatedUserId the caller's id (from the JWT)
     * @param projectId           the requested project's id
     * @return the owned project
     * @throws ProjectNotFoundException     when no project has the given id
     * @throws ProjectAccessDeniedException when the caller is not the owner
     */
    private Project findOwnedProject(UUID authenticatedUserId, UUID projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ProjectNotFoundException("Project not found: " + projectId));
        if (!project.getUserId().equals(authenticatedUserId)) {
            throw new ProjectAccessDeniedException("You do not have permission to modify this project");
        }
        return project;
    }
}
