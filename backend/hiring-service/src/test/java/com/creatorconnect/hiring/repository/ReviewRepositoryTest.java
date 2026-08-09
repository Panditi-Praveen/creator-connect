package com.creatorconnect.hiring.repository;

import com.creatorconnect.hiring.config.JpaAuditingConfig;
import com.creatorconnect.hiring.entity.Review;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Repository-layer tests for {@link ReviewRepository} running against the
 * in-memory H2 database.
 *
 * <p>{@link JpaAuditingConfig} is imported so {@code createdAt} /
 * {@code updatedAt} are populated exactly like in production. These tests
 * verify the freelancer listing (newest first), the duplicate existence check,
 * the CHAR(36) UUID mapping and the database-level unique constraint on
 * (project_id, freelancer_id).
 */
@DataJpaTest
@Import(JpaAuditingConfig.class)
class ReviewRepositoryTest {

    private static final UUID PROJECT_A = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID PROJECT_B = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID CREATOR_1 = UUID.fromString("9d2b4f79-c75f-68ff-d402-6ea024a1bd13");
    private static final UUID FREELANCER_1 = UUID.fromString("7b092f57-a53d-46dd-b2e0-4c8f0289fb91");
    private static final UUID FREELANCER_2 = UUID.fromString("8c1a3e68-b64e-57ee-c3f1-5d9f1390ac02");

    @Autowired
    private ReviewRepository reviewRepository;

    @Test
    void save_populatesIdAndAuditTimestamps() {
        Review saved = reviewRepository.save(review(PROJECT_A, FREELANCER_1));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
        assertThat(saved.getCreatorId()).isEqualTo(CREATOR_1);
        assertThat(saved.getRating()).isEqualTo(5);

        Optional<Review> reloaded = reviewRepository.findById(saved.getId());
        assertThat(reloaded).isPresent();
        assertThat(reloaded.get().getProjectId()).isEqualTo(PROJECT_A);
        assertThat(reloaded.get().getFreelancerId()).isEqualTo(FREELANCER_1);
        assertThat(reloaded.get().getReviewText())
                .isEqualTo("Outstanding work, delivered ahead of schedule.");
    }

    @Test
    void findByFreelancerId_returnsOnlyThatFreelancersReviews() {
        Review mine = reviewRepository.save(review(PROJECT_A, FREELANCER_1));
        reviewRepository.save(review(PROJECT_B, FREELANCER_2));

        List<Review> result = reviewRepository.findByFreelancerIdOrderByCreatedAtDescIdDesc(FREELANCER_1);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(mine.getId());
    }

    @Test
    void findByFreelancerId_isSortedNewestFirstDeterministic() {
        Review older = reviewRepository.save(review(PROJECT_A, FREELANCER_1));
        Review newer = reviewRepository.save(review(PROJECT_B, FREELANCER_1));

        List<Review> result = reviewRepository.findByFreelancerIdOrderByCreatedAtDescIdDesc(FREELANCER_1);

        // createdAt is written from LocalDateTime.now(), whose resolution may
        // be coarser than the gap between two successive saves, so equal
        // timestamps are possible. Assert the full ordering contract of the
        // query (createdAt DESC, then id DESC as a deterministic tiebreaker),
        // comparing the id as its STRING form to mirror the DB's CHAR(36)
        // lexicographic ordering.
        assertThat(result).isSortedAccordingTo(Comparator
                .comparing(Review::getCreatedAt).reversed()
                .thenComparing(review -> review.getId().toString(),
                        Comparator.reverseOrder()));
        assertThat(result).contains(older, newer);
    }

    @Test
    void existsByProjectIdAndFreelancerId_reflectsStoredReviews() {
        reviewRepository.save(review(PROJECT_A, FREELANCER_1));

        assertThat(reviewRepository.existsByProjectIdAndFreelancerId(PROJECT_A, FREELANCER_1)).isTrue();
        assertThat(reviewRepository.existsByProjectIdAndFreelancerId(PROJECT_A, FREELANCER_2)).isFalse();
        assertThat(reviewRepository.existsByProjectIdAndFreelancerId(PROJECT_B, FREELANCER_1)).isFalse();
    }

    @Test
    void uniqueConstraint_rejectsDuplicateProjectFreelancerPair() {
        reviewRepository.saveAndFlush(review(PROJECT_A, FREELANCER_1));

        assertThatThrownBy(() ->
                reviewRepository.saveAndFlush(review(PROJECT_A, FREELANCER_1)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void delete_removesReview() {
        Review saved = reviewRepository.save(review(PROJECT_A, FREELANCER_1));

        reviewRepository.deleteById(saved.getId());

        assertThat(reviewRepository.findById(saved.getId())).isEmpty();
    }

    private Review review(UUID projectId, UUID freelancerId) {
        return Review.builder()
                .projectId(projectId)
                .creatorId(CREATOR_1)
                .freelancerId(freelancerId)
                .rating(5)
                .reviewText("Outstanding work, delivered ahead of schedule.")
                .build();
    }
}
