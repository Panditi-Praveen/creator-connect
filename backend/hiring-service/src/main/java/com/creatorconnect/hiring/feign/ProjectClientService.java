package com.creatorconnect.hiring.feign;

import com.creatorconnect.hiring.exception.ApplicationStatusConflictException;
import com.creatorconnect.hiring.exception.ProjectNotFoundException;
import feign.FeignException;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Small façade over {@link ProjectClient} that turns raw Feign failures into
 * the Hiring Service's own exceptions.
 *
 * <p>The application and review services both need to (a) prove a project
 * exists and (b) read its owner/status; both paths go through
 * {@link #getProject}. The project lifecycle transition triggered by an
 * accepted application goes through {@link #updateProjectStatus}. Keeping the
 * Feign calls — and their {@code 404} / {@code 409} translations — in one
 * place avoids duplicating the error handling across {@code service.impl}
 * classes.
 *
 * <p>Other {@link FeignException}s (connection failures, Project Service
 * {@code 5xx}…) are intentionally left to propagate: the global exception
 * handler maps them to {@code 503 SERVICE_UNAVAILABLE}.
 */
@Service
public class ProjectClientService {

    private final ProjectClient projectClient;

    /**
     * Creates the façade with its Feign client.
     *
     * @param projectClient the declarative Project Service client
     */
    public ProjectClientService(ProjectClient projectClient) {
        this.projectClient = projectClient;
    }

    /**
     * Fetches the project with the given id.
     *
     * @param projectId the project's id
     * @return the project (never {@code null})
     * @throws ProjectNotFoundException when the project does not exist (the
     *         Project Service answered {@code 404}, or returned an empty
     *         payload)
     */
    public ProjectResponse getProject(UUID projectId) {
        try {
            ProjectApiResponse<ProjectResponse> response = projectClient.getProject(projectId);
            if (response == null || response.getData() == null) {
                throw new ProjectNotFoundException("Project not found: " + projectId);
            }
            return response.getData();
        } catch (FeignException.NotFound ex) {
            throw new ProjectNotFoundException("Project not found: " + projectId);
        }
    }

    /**
     * Moves the project with the given id to the requested lifecycle state.
     *
     * <p>Called by the application service when a creator accepts an
     * application (the project becomes {@code IN_PROGRESS}). The Project
     * Service owns and validates its own state machine — this method only
     * translates its answers:
     * <ul>
     *   <li>{@code 404} &rarr; {@link ProjectNotFoundException}</li>
     *   <li>{@code 409} &rarr; {@link ApplicationStatusConflictException}
     *       (the project is in a state that cannot move to the requested one,
     *       e.g. it is already completed)</li>
     *   <li>anything else (connection failures, {@code 5xx}) &rarr; propagates
     *       as a raw {@link FeignException}, mapped to {@code 503} by the
     *       global exception handler</li>
     * </ul>
     *
     * @param projectId the project's id
     * @param status    the requested lifecycle state
     * @throws ProjectNotFoundException        when the project does not exist
     * @throws ApplicationStatusConflictException when the transition is not
     *         allowed by the project's current state
     */
    public void updateProjectStatus(UUID projectId, ProjectStatus status) {
        try {
            projectClient.updateProjectStatus(projectId, new UpdateProjectStatusRequest(status));
        } catch (FeignException.NotFound ex) {
            throw new ProjectNotFoundException("Project not found: " + projectId);
        } catch (FeignException.Conflict ex) {
            throw new ApplicationStatusConflictException(
                    "The project does not allow this status change (it is likely already completed or cancelled)");
        }
    }
}
