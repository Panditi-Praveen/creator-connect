package com.creatorconnect.project.mapper;

import com.creatorconnect.project.dto.request.ProjectRequest;
import com.creatorconnect.project.entity.Project;
import com.creatorconnect.project.entity.ProjectStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ProjectMapper#toEntity} — the create-time mapping
 * rules. The service layer rejects non-{@code OPEN} supplied statuses before
 * this mapper runs; here the mapper's own contract is verified: an omitted
 * status defaults to {@code OPEN}, and an explicit {@code OPEN} passes
 * through.
 */
class ProjectMapperTest {

    private static final UUID USER_ID = UUID.fromString("7b092f57-a53d-46dd-b2e0-4c8f0289fb91");

    private final ProjectMapper projectMapper = new ProjectMapper();

    @Test
    void toEntity_withoutStatus_defaultsToOpen() {
        Project project = projectMapper.toEntity(USER_ID, projectRequest(null));

        assertThat(project.getStatus()).isEqualTo(ProjectStatus.OPEN);
        assertThat(project.getUserId()).isEqualTo(USER_ID);
    }

    @Test
    void toEntity_withOpenStatus_keepsOpen() {
        Project project = projectMapper.toEntity(USER_ID, projectRequest(ProjectStatus.OPEN));

        assertThat(project.getStatus()).isEqualTo(ProjectStatus.OPEN);
    }

    private ProjectRequest projectRequest(ProjectStatus status) {
        return ProjectRequest.builder()
                .title("YouTube Intro Package")
                .description("Need a 15-second animated intro for a new YouTube channel.")
                .category("Video Editing")
                .skillsRequired(List.of("After Effects", "Motion Design"))
                .budget(new BigDecimal("500.00"))
                .duration("1 week")
                .experienceLevel("Intermediate")
                .location("Remote")
                .status(status)
                .applicationDeadline(LocalDate.now().plusDays(30))
                .build();
    }
}
