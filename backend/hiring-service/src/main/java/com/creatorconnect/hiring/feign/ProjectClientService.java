package com.creatorconnect.hiring.feign;

import com.creatorconnect.hiring.exception.ProjectNotFoundException;
import feign.FeignException;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Small façade over {@link ProjectClient} that turns raw Feign failures into
 * the Hiring Service's own exceptions.
 *
 * <p>The application and review services both need to (a) prove a project
 * exists and (b) read its owner; both paths go through {@link #getProject}.
 * Keeping the Feign call — and its {@code 404} translation — in one place
 * avoids duplicating the error handling across {@code service.impl} classes.
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
}
