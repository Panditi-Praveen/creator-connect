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
import java.util.Comparator;
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
 * verify the two filtered listing queries, the JSON round-trip of
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
    void findByUserIdAndFilters_returnsOnlyThatUsersProjects() {
        Project mine = projectRepository.save(project("My project", USER_A));
        projectRepository.save(project("Someone else's project", USER_B));

        List<Project> result = projectRepository.findByUserIdAndFilters(
                USER_A, null, null, null, null, null, null, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(mine.getId());
        assertThat(result.get(0).getTitle()).isEqualTo("My project");
    }

    @Test
    void findAllByFilters_ordersNewestFirst() {
        Project older = projectRepository.save(project("Older project", USER_A));
        Project newer = projectRepository.save(project("Newer project", USER_B));

        List<Project> result = projectRepository.findAllByFilters(
                null, null, null, null, null, null, null);

        // Both projects are returned. `createdAt` is written from
        // LocalDateTime.now(), whose resolution may be coarser than the gap
        // between two successive saves, so equal timestamps are possible.
        // Instead of assuming distinct timestamps, this assertion verifies the
        // full ordering contract of the query (createdAt DESC, then id DESC as
        // a deterministic tiebreaker) — it is flake-free at any clock
        // resolution and would fail if the id DESC tiebreaker were removed.
        //
        // The id tiebreaker must be compared as the id's STRING form: the DB
        // stores id as CHAR(36) and orders it lexicographically, whereas
        // UUID.compareTo() uses signed 128-bit comparison — the two disagree
        // for random UUIDs that straddle the sign boundary (first hex digit
        // >= 8). Comparing toString() mirrors exactly what the database does.
        assertThat(result).hasSize(2);
        assertThat(result).isSortedAccordingTo(Comparator
                .comparing(Project::getCreatedAt).reversed()
                .thenComparing(project -> project.getId().toString(),
                        Comparator.reverseOrder()));
    }

    @Test
    void findAllByFilters_withNoFilters_returnsAllProjects() {
        projectRepository.save(project("Project A", USER_A));
        projectRepository.save(project("Project B", USER_B));

        List<Project> result = projectRepository.findAllByFilters(
                null, null, null, null, null, null, null);

        assertThat(result).hasSize(2);
    }

    @Test
    void findAllByFilters_withCategoryFilter_returnsOnlyMatching() {
        projectRepository.save(project(
                "Video project", "A video edit.",
                "Video Editing", List.of("Premiere Pro"), new BigDecimal("400.00"), USER_A));
        Project graphic = projectRepository.save(project(
                "Graphic project", "A brand identity.",
                "Graphic Design", List.of("Illustrator"), new BigDecimal("300.00"), USER_A));

        List<Project> result = projectRepository.findAllByFilters(
                "graphic design", null, null, null, null, null, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(graphic.getId());
    }

    @Test
    void findAllByFilters_withSkillFilter_returnsOnlyMatching() {
        Project matching = projectRepository.save(project(
                "Motion project", "A motion reel.",
                "Video Editing", List.of("After Effects", "3D Animation"), new BigDecimal("800.00"), USER_A));
        projectRepository.save(project(
                "Writing project", "A script.",
                "Copywriting", List.of("Storytelling"), new BigDecimal("200.00"), USER_B));

        List<Project> result = projectRepository.findAllByFilters(
                null, "after effects", null, null, null, null, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(matching.getId());
    }

    @Test
    void findAllByFilters_withBudgetRange_returnsOnlyMatching() {
        projectRepository.save(project(
                "Cheap project", "Low budget.",
                "Video Editing", List.of("Editing"), new BigDecimal("100.00"), USER_A));
        Project mid = projectRepository.save(project(
                "Mid project", "Mid budget.",
                "Video Editing", List.of("Editing"), new BigDecimal("500.00"), USER_A));
        projectRepository.save(project(
                "Pricy project", "High budget.",
                "Video Editing", List.of("Editing"), new BigDecimal("2000.00"), USER_B));

        List<Project> result = projectRepository.findAllByFilters(
                null, null, new BigDecimal("400.00"), new BigDecimal("1500.00"), null, null, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(mid.getId());
    }

    @Test
    void findAllByFilters_withKeyword_returnsOnlyMatching() {
        Project titled = projectRepository.save(project(
                "YouTube channel intro", "An animated opener.",
                "Video Editing", List.of("After Effects"), new BigDecimal("500.00"), USER_A));
        projectRepository.save(project(
                "Podcast cover", "An eye-catching cover.",
                "Graphic Design", List.of("Photoshop"), new BigDecimal("250.00"), USER_B));

        List<Project> result = projectRepository.findAllByFilters(
                null, null, null, null, null, null, "youtube");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(titled.getId());
    }

    @Test
    void findAllByFilters_withCombinedFilters_returnsOnlyMatching() {
        projectRepository.save(project(
                "Vlog edit", "A vlog.",
                "Video Editing", List.of("Premiere Pro"), new BigDecimal("300.00"), USER_A));
        Project matching = projectRepository.save(project(
                "After Effects intro", "A motion intro.",
                "Video Editing", List.of("After Effects"), new BigDecimal("600.00"), USER_A));

        List<Project> result = projectRepository.findAllByFilters(
                "video editing", "after effects", new BigDecimal("400.00"), null, null, null, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(matching.getId());
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
        return project(title, "A short description of the project.", "Video Editing",
                List.of("After Effects", "Motion Design"), new BigDecimal("500.00"), userId);
    }

    private Project project(String title, String description, String category,
                            List<String> skills, BigDecimal budget, UUID userId) {
        return Project.builder()
                .userId(userId)
                .title(title)
                .description(description)
                .category(category)
                .skillsRequired(skills)
                .budget(budget)
                .duration("1 week")
                .experienceLevel("Intermediate")
                .location("Remote")
                .status(ProjectStatus.OPEN)
                .applicationDeadline(LocalDate.now().plusDays(30))
                .build();
    }
}
