package com.creatorconnect.hiring.repository;

import com.creatorconnect.hiring.config.JpaAuditingConfig;
import com.creatorconnect.hiring.entity.Application;
import com.creatorconnect.hiring.entity.ApplicationStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Repository-layer tests for {@link ApplicationRepository} running against the
 * in-memory H2 database.
 *
 * <p>{@link JpaAuditingConfig} is imported so {@code createdAt} /
 * {@code updatedAt} are populated exactly like in production. These tests
 * verify the freelancer/project listings (with pagination), the duplicate
 * existence check, the CHAR(36) UUID mapping and the database-level unique
 * constraint on (project_id, freelancer_id).
 */
@DataJpaTest
@Import(JpaAuditingConfig.class)
class ApplicationRepositoryTest {

    private static final UUID PROJECT_A = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID PROJECT_B = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID FREELANCER_1 = UUID.fromString("7b092f57-a53d-46dd-b2e0-4c8f0289fb91");
    private static final UUID FREELANCER_2 = UUID.fromString("8c1a3e68-b64e-57ee-c3f1-5d9f1390ac02");

    @Autowired
    private ApplicationRepository applicationRepository;

    @Test
    void save_populatesIdAndAuditTimestamps_andDefaultsToPending() {
        Application saved = applicationRepository.save(application(PROJECT_A, FREELANCER_1));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
        assertThat(saved.getStatus()).isEqualTo(ApplicationStatus.PENDING);
        assertThat(saved.getFreelancerId()).isEqualTo(FREELANCER_1);

        Optional<Application> reloaded = applicationRepository.findById(saved.getId());
        assertThat(reloaded).isPresent();
        assertThat(reloaded.get().getProjectId()).isEqualTo(PROJECT_A);
        assertThat(reloaded.get().getProposal()).isEqualTo("I can deliver this in 2 weeks.");
    }

    @Test
    void findByFreelancerId_returnsOnlyThatFreelancersApplications() {
        Application mine = applicationRepository.save(application(PROJECT_A, FREELANCER_1));
        applicationRepository.save(application(PROJECT_B, FREELANCER_2));

        List<Application> result = applicationRepository.findByFreelancerId(FREELANCER_1);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(mine.getId());
    }

    @Test
    void findByProjectId_returnsOnlyThatProjectsApplications() {
        Application mine = applicationRepository.save(application(PROJECT_A, FREELANCER_1));
        applicationRepository.save(application(PROJECT_B, FREELANCER_2));

        List<Application> result = applicationRepository.findByProjectId(PROJECT_A);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(mine.getId());
    }

    @Test
    void existsByProjectIdAndFreelancerId_reflectsStoredApplications() {
        applicationRepository.save(application(PROJECT_A, FREELANCER_1));

        assertThat(applicationRepository.existsByProjectIdAndFreelancerId(PROJECT_A, FREELANCER_1)).isTrue();
        assertThat(applicationRepository.existsByProjectIdAndFreelancerId(PROJECT_A, FREELANCER_2)).isFalse();
        assertThat(applicationRepository.existsByProjectIdAndFreelancerId(PROJECT_B, FREELANCER_1)).isFalse();
    }

    @Test
    void paginatedListing_returnsRequestedPage() {
        applicationRepository.save(application(PROJECT_A, FREELANCER_1));
        applicationRepository.save(application(PROJECT_A, FREELANCER_2));
        applicationRepository.save(application(PROJECT_B, FREELANCER_1));

        Page<Application> firstPage = applicationRepository.findByFreelancerId(
                FREELANCER_1, PageRequest.of(0, 1));

        assertThat(firstPage.getTotalElements()).isEqualTo(2);
        assertThat(firstPage.getTotalPages()).isEqualTo(2);
        assertThat(firstPage.getContent()).hasSize(1);

        Page<Application> secondPage = applicationRepository.findByFreelancerId(
                FREELANCER_1, PageRequest.of(1, 1));

        assertThat(secondPage.getContent()).hasSize(1);
    }

    @Test
    void paginatedListing_respectsSortOrder_newestFirstDeterministic() {
        Application older = applicationRepository.save(application(PROJECT_A, FREELANCER_1));
        Application newer = applicationRepository.save(application(PROJECT_B, FREELANCER_1));

        Page<Application> page = applicationRepository.findByFreelancerId(
                FREELANCER_1,
                PageRequest.of(0, 20, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))));

        // createdAt is written from LocalDateTime.now(), whose resolution may
        // be coarser than the gap between two successive saves, so equal
        // timestamps are possible. Assert the full ordering contract of the
        // page (createdAt DESC, then id DESC as a deterministic tiebreaker),
        // comparing the id as its STRING form to mirror the DB's CHAR(36)
        // lexicographic ordering.
        assertThat(page.getContent()).isSortedAccordingTo(Comparator
                .comparing(Application::getCreatedAt).reversed()
                .thenComparing(application -> application.getId().toString(),
                        Comparator.reverseOrder()));
        assertThat(page.getContent()).contains(older, newer);
    }

    @Test
    void uniqueConstraint_rejectsDuplicateProjectFreelancerPair() {
        applicationRepository.saveAndFlush(application(PROJECT_A, FREELANCER_1));

        assertThatThrownBy(() ->
                applicationRepository.saveAndFlush(application(PROJECT_A, FREELANCER_1)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void delete_removesApplication() {
        Application saved = applicationRepository.save(application(PROJECT_A, FREELANCER_1));

        applicationRepository.deleteById(saved.getId());

        assertThat(applicationRepository.findById(saved.getId())).isEmpty();
    }

    @Test
    void findById_missing_returnsEmpty() {
        assertThat(applicationRepository.findById(UUID.randomUUID())).isEmpty();
    }

    private Application application(UUID projectId, UUID freelancerId) {
        return Application.builder()
                .projectId(projectId)
                .freelancerId(freelancerId)
                .proposal("I can deliver this in 2 weeks.")
                .expectedBudget(new BigDecimal("500.00"))
                .estimatedDuration("2 weeks")
                .build();
    }
}
