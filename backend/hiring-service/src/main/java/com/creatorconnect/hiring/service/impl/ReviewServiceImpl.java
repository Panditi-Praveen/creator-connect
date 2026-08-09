package com.creatorconnect.hiring.service.impl;

import com.creatorconnect.hiring.dto.request.ReviewRequest;
import com.creatorconnect.hiring.dto.response.FreelancerReviewsResponse;
import com.creatorconnect.hiring.dto.response.ReviewResponse;
import com.creatorconnect.hiring.entity.ApplicationStatus;
import com.creatorconnect.hiring.entity.Review;
import com.creatorconnect.hiring.exception.DuplicateReviewException;
import com.creatorconnect.hiring.exception.ReviewAccessDeniedException;
import com.creatorconnect.hiring.exception.ReviewValidationException;
import com.creatorconnect.hiring.feign.ProjectClientService;
import com.creatorconnect.hiring.feign.ProjectResponse;
import com.creatorconnect.hiring.mapper.ReviewMapper;
import com.creatorconnect.hiring.repository.ApplicationRepository;
import com.creatorconnect.hiring.repository.ReviewRepository;
import com.creatorconnect.hiring.service.ReviewService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Concrete {@link ReviewService} implementation.
 *
 * <p>Owns the review lifecycle with these rules:
 * <ol>
 *   <li><b>Creator &amp; owner only</b> — only {@code CREATOR}s may submit
 *       reviews, and only on projects they own (the project's owner is
 *       fetched from the Project Service via OpenFeign); the caller's
 *       {@code userId} (from the JWT) becomes the {@code creatorId}.</li>
 *   <li><b>Hired first</b> — the freelancer must hold an {@code ACCEPTED}
 *       application on the project (verified against this service's own
 *       {@code applications} table) before they can be reviewed; otherwise the
 *       review is rejected ({@link ReviewValidationException}).</li>
 *   <li><b>One review per pair</b> — a second review for the same
 *       {@code (project, freelancer)} pair is rejected
 *       ({@link DuplicateReviewException}).</li>
 *   <li><b>Public reading</b> — anyone authenticated may read a freelancer's
 *       reviews and average rating.</li>
 * </ol>
 *
 * <p>Dependencies are injected through the constructor only (no field
 * injection). Write operations run inside one {@code @Transactional} boundary
 * so a failure rolls back cleanly.
 */
@Service
public class ReviewServiceImpl implements ReviewService {

    private static final String ROLE_CREATOR = "CREATOR";

    private final ReviewRepository reviewRepository;
    private final ApplicationRepository applicationRepository;
    private final ReviewMapper reviewMapper;
    private final ProjectClientService projectClientService;

    /**
     * Creates the service with its collaborators.
     *
     * @param reviewRepository       the review data access layer
     * @param applicationRepository  the application data access layer (used to
     *                               verify the freelancer was hired)
     * @param reviewMapper           the entity/DTO mapper
     * @param projectClientService   the Project Service client used to verify
     *                               the creator owns the project
     */
    public ReviewServiceImpl(ReviewRepository reviewRepository,
                             ApplicationRepository applicationRepository,
                             ReviewMapper reviewMapper,
                             ProjectClientService projectClientService) {
        this.reviewRepository = reviewRepository;
        this.applicationRepository = applicationRepository;
        this.reviewMapper = reviewMapper;
        this.projectClientService = projectClientService;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public ReviewResponse createReview(UUID creatorId, String role, ReviewRequest request) {
        if (!ROLE_CREATOR.equalsIgnoreCase(role)) {
            throw new ReviewAccessDeniedException("Only creators can submit reviews");
        }
        if (reviewRepository.existsByProjectIdAndFreelancerId(request.getProjectId(), request.getFreelancerId())) {
            throw new DuplicateReviewException("This freelancer has already been reviewed on this project");
        }
        // Only the project's owner may review on it (404 when the project is
        // missing from the Project Service).
        ProjectResponse project = projectClientService.getProject(request.getProjectId());
        if (!project.getUserId().equals(creatorId)) {
            throw new ReviewAccessDeniedException("Only the project owner can review on this project");
        }
        boolean hired = applicationRepository.existsByProjectIdAndFreelancerIdAndStatus(
                request.getProjectId(), request.getFreelancerId(), ApplicationStatus.ACCEPTED);
        if (!hired) {
            throw new ReviewValidationException(
                    "Only freelancers with an accepted application can be reviewed on this project");
        }
        Review review = reviewRepository.save(
                reviewMapper.toEntity(creatorId, request));
        return reviewMapper.toResponse(review);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public FreelancerReviewsResponse getFreelancerReviews(UUID freelancerId) {
        List<Review> reviews = reviewRepository.findByFreelancerIdOrderByCreatedAtDescIdDesc(freelancerId);
        return FreelancerReviewsResponse.builder()
                .freelancerId(freelancerId)
                .averageRating(averageRating(reviews))
                .totalReviews(reviews.size())
                .reviews(reviews.stream()
                        .map(reviewMapper::toResponse)
                        .toList())
                .build();
    }

    /**
     * Computes the arithmetic mean of the reviews' ratings, rounded to one
     * decimal place.
     *
     * @param reviews the freelancer's reviews (may be empty)
     * @return the rounded average, or {@code 0.0} when there are no reviews
     */
    private double averageRating(List<Review> reviews) {
        if (reviews.isEmpty()) {
            return 0.0;
        }
        double average = reviews.stream()
                .mapToInt(Review::getRating)
                .average()
                .orElse(0.0);
        return Math.round(average * 10.0) / 10.0;
    }
}
