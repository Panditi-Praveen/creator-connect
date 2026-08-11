package com.creatorconnect.hiring.feign;

import com.creatorconnect.hiring.exception.ApplicationStatusConflictException;
import com.creatorconnect.hiring.exception.ProjectNotFoundException;
import feign.FeignException;
import feign.Request;
import feign.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ProjectClientService} — the Project Service façade
 * that unwraps the success envelope and translates the Project Service's
 * answers into the Hiring Service's own exceptions: a {@code 404} becomes
 * {@link ProjectNotFoundException} and a {@code 409} on a status transition
 * becomes {@link ApplicationStatusConflictException}.
 */
@ExtendWith(MockitoExtension.class)
class ProjectClientServiceTest {

    private static final UUID PROJECT_ID = UUID.fromString("9d2b4f79-c75f-68ff-d402-6ea024a1bd13");

    @Mock
    private ProjectClient projectClient;

    @Test
    void getProject_whenProjectExists_returnsUnwrappedProject() {
        ProjectResponse project = new ProjectResponse(PROJECT_ID, UUID.randomUUID(), ProjectStatus.OPEN);
        when(projectClient.getProject(PROJECT_ID))
                .thenReturn(new ProjectApiResponse<>(null, 200, "Project retrieved successfully",
                        project, "/projects/" + PROJECT_ID));

        ProjectResponse result = new ProjectClientService(projectClient).getProject(PROJECT_ID);

        assertThat(result.getId()).isEqualTo(PROJECT_ID);
        assertThat(result.getUserId()).isEqualTo(project.getUserId());
        assertThat(result.getStatus()).isEqualTo(ProjectStatus.OPEN);
    }

    @Test
    void getProject_whenProjectServiceAnswers404_throwsProjectNotFound() {
        when(projectClient.getProject(PROJECT_ID)).thenThrow(notFoundException());

        assertThatThrownBy(() -> new ProjectClientService(projectClient).getProject(PROJECT_ID))
                .isInstanceOf(ProjectNotFoundException.class)
                .hasMessageContaining(PROJECT_ID.toString());
    }

    @Test
    void getProject_whenPayloadIsEmpty_throwsProjectNotFound() {
        when(projectClient.getProject(PROJECT_ID))
                .thenReturn(new ProjectApiResponse<>(null, 200, "ok", null, "/projects/" + PROJECT_ID));

        assertThatThrownBy(() -> new ProjectClientService(projectClient).getProject(PROJECT_ID))
                .isInstanceOf(ProjectNotFoundException.class);
    }

    @Test
    void updateProjectStatus_whenAccepted_delegatesWithTheNewStatus() {
        ProjectClientService service = new ProjectClientService(projectClient);
        when(projectClient.updateProjectStatus(eq(PROJECT_ID), eq(new UpdateProjectStatusRequest(ProjectStatus.IN_PROGRESS))))
                .thenReturn(new ProjectApiResponse<>(null, 200, "Project status updated successfully",
                        new ProjectResponse(PROJECT_ID, UUID.randomUUID(), ProjectStatus.IN_PROGRESS),
                        "/projects/" + PROJECT_ID + "/status"));

        service.updateProjectStatus(PROJECT_ID, ProjectStatus.IN_PROGRESS);

        verify(projectClient).updateProjectStatus(PROJECT_ID, new UpdateProjectStatusRequest(ProjectStatus.IN_PROGRESS));
    }

    @Test
    void updateProjectStatus_whenProjectServiceAnswers404_throwsProjectNotFound() {
        when(projectClient.updateProjectStatus(eq(PROJECT_ID), eq(new UpdateProjectStatusRequest(ProjectStatus.IN_PROGRESS))))
                .thenThrow(notFoundException());

        assertThatThrownBy(() -> new ProjectClientService(projectClient)
                .updateProjectStatus(PROJECT_ID, ProjectStatus.IN_PROGRESS))
                .isInstanceOf(ProjectNotFoundException.class)
                .hasMessageContaining(PROJECT_ID.toString());
    }

    @Test
    void updateProjectStatus_whenProjectServiceAnswers409_throwsConflict() {
        when(projectClient.updateProjectStatus(eq(PROJECT_ID), eq(new UpdateProjectStatusRequest(ProjectStatus.IN_PROGRESS))))
                .thenThrow(conflictException());

        assertThatThrownBy(() -> new ProjectClientService(projectClient)
                .updateProjectStatus(PROJECT_ID, ProjectStatus.IN_PROGRESS))
                .isInstanceOf(ApplicationStatusConflictException.class);
    }

    /**
     * Builds a realistic {@code FeignException.NotFound} exactly as Feign
     * would after the Project Service answers {@code 404}.
     *
     * @return the Feign {@code 404} exception
     */
    private FeignException.NotFound notFoundException() {
        return (FeignException.NotFound) feignException(404);
    }

    /**
     * Builds a realistic {@code FeignException.Conflict} exactly as Feign
     * would after the Project Service answers {@code 409}.
     *
     * @return the Feign {@code 409} exception
     */
    private FeignException.Conflict conflictException() {
        return (FeignException.Conflict) feignException(409);
    }

    private FeignException feignException(int status) {
        Request request = Request.create(Request.HttpMethod.GET,
                "http://localhost:8083/projects/" + PROJECT_ID,
                Map.of(), null, StandardCharsets.UTF_8);
        Response response = Response.builder()
                .status(status)
                .reason("error")
                .request(request)
                .headers(Map.of())
                .build();
        return FeignException.errorStatus("ProjectClient#request(UUID)", response);
    }
}
