package com.creatorconnect.project.service;

import com.creatorconnect.project.dto.request.ProjectFilter;
import com.creatorconnect.project.dto.request.ProjectRequest;
import com.creatorconnect.project.dto.request.UpdateProjectRequest;
import com.creatorconnect.project.dto.response.ProjectResponse;
import com.creatorconnect.project.entity.ProjectStatus;

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
     * Returns the browse feed, most recently created first, filtered by the
     * given optional criteria.
     *
     * @param filter the optional feed filters (every field {@code null}
     *               returns the unfiltered feed)
     * @return the filtered browse feed
     */
    List<ProjectResponse> getAllProjects(ProjectFilter filter);

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
     * first, filtered by the same optional criteria as the browse feed.
     *
     * @param userId the owning user's id (from the JWT)
     * @param filter the optional feed filters (every field {@code null}
     *               returns all of the user's projects)
     * @return the user's filtered project projections
     */
    List<ProjectResponse> getProjectsByUserId(UUID userId, ProjectFilter filter);

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

    /**
     * Moves the project through its lifecycle state machine.
     *
     * <p>Only forward transitions are allowed
     * ({@code OPEN &rarr; IN_PROGRESS / COMPLETED / CANCELLED},
     * {@code IN_PROGRESS &rarr; COMPLETED / CANCELLED}); re-applying the
     * current status is an idempotent no-op; terminal states
     * ({@code COMPLETED}, {@code CANCELLED}) are locked. This is the endpoint
     * the Hiring Service calls (via OpenFeign) when a creator accepts an
     * application, moving the project to {@code IN_PROGRESS} automatically.
     *
     * @param authenticatedUserId the caller's id (from the JWT)
     * @param projectId           the project to transition
     * @param newStatus           the requested lifecycle state
     * @return the updated project projection
     * @throws com.creatorconnect.project.exception.ProjectNotFoundException
     *         when no project has the given id
     * @throws com.creatorconnect.project.exception.ProjectAccessDeniedException
     *         when the caller is not the project owner
     * @throws com.creatorconnect.project.exception.ProjectStatusConflictException
     *         when the transition is not allowed by the state machine
     */
    ProjectResponse updateProjectStatus(UUID authenticatedUserId, UUID projectId, ProjectStatus newStatus);
}
