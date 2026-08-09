package com.creatorconnect.hiring.repository;

import com.creatorconnect.hiring.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for the {@link Review} entity.
 *
 * <p>Provides CRUD access plus the queries backing the freelancer listing and
 * the duplicate guard:
 * <ul>
 *   <li>{@link #findByFreelancerIdOrderByCreatedAtDescIdDesc} — a freelancer's
 *       reviews for {@code GET /reviews/freelancer/{freelancerId}}, newest
 *       first with a deterministic {@code id DESC} tiebreak. Unlike the
 *       application listings, the order is hard-coded in the derived method
 *       name rather than taken from a {@code Pageable} because this endpoint
 *       returns the whole (non-paginated) aggregate together with the average
 *       rating.</li>
 *   <li>{@link #existsByProjectIdAndFreelancerId} — the duplicate-review
 *       guard, mirrored by the {@code unique} constraint on
 *       {@code (project_id, freelancer_id)} at the database level.</li>
 * </ul>
 */
public interface ReviewRepository extends JpaRepository<Review, UUID> {

    /**
     * Returns all reviews received by the given freelancer, newest first with
     * a deterministic {@code id DESC} tiebreak.
     *
     * @param freelancerId the freelancer's id (matches the JWT {@code userId}
     *                     claim)
     * @return the freelancer's reviews, newest first
     */
    List<Review> findByFreelancerIdOrderByCreatedAtDescIdDesc(UUID freelancerId);

    /**
     * Reports whether the freelancer was already reviewed on the project.
     *
     * @param projectId    the project's id
     * @param freelancerId the freelancer's id
     * @return {@code true} when a review already exists for the pair
     */
    boolean existsByProjectIdAndFreelancerId(UUID projectId, UUID freelancerId);
}
