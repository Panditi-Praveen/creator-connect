package com.creatorconnect.project.service;

import com.creatorconnect.project.dto.request.ProjectRequest;
import com.creatorconnect.project.dto.request.UpdateProjectRequest;
import com.creatorconnect.project.dto.response.ProjectResponse;

import java.util.List;
import java.util.UUID;

/**
 * Project Service use cases — the business logic contract layer.
 *
 * <p>Exposes the operations the Project Service API supports. Implementations
 * live in {@code service.impl}; the interface decouples the controller from
 * concrete logic (SOLID — dependency inversion).
 *
 * <p>Ownership discipline: the {@code authenticatedUserId} parameters are
 * always derived from the JWT, never from the request body, so a caller can
 * only create/update/delete their own projects.
 */
public interface ProjectService {

    /**
     * Creates a project owned by the given authenticated user.
     *
     * @param userId  the owning user's id (from the JWT)
     * @param request the validated create payload
     * @return the persisted project projection
     */
    ProjectResponse createProject(UUID userId, ProjectRequest request);

    /**
     * Returns every project, most recently created first.
     *
     * @return the browse feed
     */
    List<ProjectResponse> getAllProjects();

    /**
     * Loads a single project by id (any authenticated caller may view any
     * project).
     *
     * @param projectId the project's id
     * @return the persisted project projection
     * @throws com.creatorconnect.project.exception.ProjectNotFoundException
     *         when no project has the given id
     */
    ProjectResponse getProjectById(UUID projectId);

    /**
     * Returns all projects owned by the given user, most recently created
     * first.
     *
     * @param userId the owning user's id (from the JWT)
     * @return the user's project projections
     */
    List<ProjectResponse> getProjectsByUserId(UUID userId);

    /**
     * Updates the project with the given id.
     *
     * @param authenticatedUserId the caller's id (from the JWT)
     * @param projectId           the project to update
     * @param request             the validated partial-update payload
     * @return the updated project projection
     * @throws com.creatorconnect.project.exception.ProjectNotFoundException
     *         when no project has the given id
     * @throws com.creatorconnect.project.exception.ProjectAccessDeniedException
     *         when the caller is not the project owner
     */
    ProjectResponse updateProject(UUID authenticatedUserId, UUID projectId, UpdateProjectRequest request);

    /**
     * Deletes the project with the given id.
     *
     * @param authenticatedUserId the caller's id (from the JWT)
     * @param projectId           the project to delete
     * @throws com.creatorconnect.project.exception.ProjectNotFoundException
     *         when no project has the given id
     * @throws com.creatorconnect.project.exception.ProjectAccessDeniedException
     *         when the caller is not the project owner
     */
    void deleteProject(UUID authenticatedUserId, UUID projectId);
}
