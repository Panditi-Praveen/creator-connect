package com.creatorconnect.hiring.feign;

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
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ProjectClientService} — the Project Service façade
 * that unwraps the success envelope and translates a {@code 404} answer from
 * the Project Service into the Hiring Service's own
 * {@link ProjectNotFoundException}.
 */
@ExtendWith(MockitoExtension.class)
class ProjectClientServiceTest {

    private static final UUID PROJECT_ID = UUID.fromString("9d2b4f79-c75f-68ff-d402-6ea024a1bd13");

    @Mock
    private ProjectClient projectClient;

    @Test
    void getProject_whenProjectExists_returnsUnwrappedProject() {
        ProjectResponse project = new ProjectResponse(PROJECT_ID, UUID.randomUUID());
        when(projectClient.getProject(PROJECT_ID))
                .thenReturn(new ProjectApiResponse<>(null, 200, "Project retrieved successfully",
                        project, "/projects/" + PROJECT_ID));

        ProjectResponse result = new ProjectClientService(projectClient).getProject(PROJECT_ID);

        assertThat(result.getId()).isEqualTo(PROJECT_ID);
        assertThat(result.getUserId()).isEqualTo(project.getUserId());
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

    /**
     * Builds a realistic {@code FeignException.NotFound} exactly as Feign
     * would after the Project Service answers {@code 404}.
     *
     * @return the Feign {@code 404} exception
     */
    private FeignException.NotFound notFoundException() {
        Request request = Request.create(Request.HttpMethod.GET,
                "http://localhost:8083/projects/" + PROJECT_ID,
                Map.of(), null, StandardCharsets.UTF_8);
        Response response = Response.builder()
                .status(404)
                .reason("Not Found")
                .request(request)
                .headers(Map.of())
                .build();
        return (FeignException.NotFound) FeignException.errorStatus("ProjectClient#getProject(UUID)", response);
    }
}
