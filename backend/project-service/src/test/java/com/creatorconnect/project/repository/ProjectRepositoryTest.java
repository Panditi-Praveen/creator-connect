package com.creatorconnect.project.repository;

import com.creatorconnect.project.config.JpaAuditingConfig;
import com.creatorconnect.project.entity.Project;
import com.creatorconnect.project.entity.ProjectStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Repository-layer tests for {@link ProjectRepository} running against the
 * in-memory H2 database.
 *
 * <p>{@link JpaAuditingConfig} is imported so {@code createdAt} /
 * {@code updatedAt} are populated exactly like in production. These tests
 * verify the two derived queries plus the JSON round-trip of
 * {@code skillsRequired} and the CHAR(36) UUID mapping.
 */
@DataJpaTest
@Import(JpaAuditingConfig.class)
class ProjectRepositoryTest {

    private static final UUID USER_A = UUID.fromString("7b092f57-a53d-46dd-b2e0-4c8f0289fb91");
    private static final UUID USER_B = UUID.fromString("8c1a3e68-b64e-57ee-c3f1-5d9f1390ac02");

    @Autowired
    private ProjectRepository projectRepository;

    @Test
    void save_populatesIdAndAuditTimestamps_andRoundTripsSkillsJson() {
        Project saved = projectRepository.save(project("YouTube Intro Package", USER_A));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
        assertThat(saved.getSkillsRequired()).containsExactly("After Effects", "Motion Design");

        Optional<Project> reloaded = projectRepository.findById(saved.getId());
        assertThat(reloaded).isPresent();
        assertThat(reloaded.get().getUserId()).isEqualTo(USER_A);
        assertThat(reloaded.get().getStatus()).isEqualTo(ProjectStatus.OPEN);
        assertThat(reloaded.get().getSkillsRequired()).containsExactly("After Effects", "Motion Design");
    }

    @Test
    void findByUserIdOrderByCreatedAtDesc_returnsOnlyThatUsersProjects() {
        Project mine = projectRepository.save(project("My project", USER_A));
        projectRepository.save(project("Someone else's project", USER_B));

        List<Project> result = projectRepository.findByUserIdOrderByCreatedAtDesc(USER_A);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(mine.getId());
        assertThat(result.get(0).getTitle()).isEqualTo("My project");
    }

    @Test
    void findAllByOrderByCreatedAtDesc_ordersNewestFirst() {
        Project older = projectRepository.save(project("Older project", USER_A));
        Project newer = projectRepository.save(project("Newer project", USER_B));

        List<Project> result = projectRepository.findAllByOrderByCreatedAtDesc();

        assertThat(result).extracting(Project::getId)
                .containsExactly(newer.getId(), older.getId());
    }

    @Test
    void delete_removesProject() {
        Project saved = projectRepository.save(project("To be deleted", USER_A));

        projectRepository.deleteById(saved.getId());

        assertThat(projectRepository.findById(saved.getId())).isEmpty();
    }

    @Test
    void findById_missing_returnsEmpty() {
        assertThat(projectRepository.findById(UUID.randomUUID())).isEmpty();
    }

    private Project project(String title, UUID userId) {
        return Project.builder()
                .userId(userId)
                .title(title)
                .description("A short description of the project.")
                .category("Video Editing")
                .skillsRequired(List.of("After Effects", "Motion Design"))
                .budget(new BigDecimal("500.00"))
                .duration("1 week")
                .experienceLevel("Intermediate")
                .location("Remote")
                .status(ProjectStatus.OPEN)
                .applicationDeadline(LocalDate.now().plusDays(30))
                .build();
    }
}
