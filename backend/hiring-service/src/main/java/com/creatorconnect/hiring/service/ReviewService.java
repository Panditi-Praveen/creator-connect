package com.creatorconnect.hiring.service;

import com.creatorconnect.hiring.dto.request.ReviewRequest;
import com.creatorconnect.hiring.dto.response.FreelancerReviewsResponse;
import com.creatorconnect.hiring.dto.response.ReviewResponse;

import java.util.UUID;

/**
 * Hiring Service review use cases — the business logic contract layer.
 *
 * <p>Exposes the operations the review API supports. Implementations live in
 * {@code service.impl}; the interface decouples the controller from concrete
 * logic (SOLID — dependency inversion).
 *
 * <p>Ownership discipline: the {@code creatorId} / {@code role} parameters
 * are always derived from the JWT, never from the request body, so a caller
 * can only review as themselves and within their own role.
 */
public interface ReviewService {

    /**
     * Submits a review on behalf of the given creator.
     *
     * <p>The creator must own the project (verified via OpenFeign against the
     * Project Service) and the freelancer must have been hired on it.
     *
     * @param creatorId the reviewing creator's id (from the JWT)
     * @param role      the caller's role from the JWT
     * @param request   the validated create payload
     * @return the persisted review projection
     * @throws com.creatorconnect.hiring.exception.ReviewAccessDeniedException
     *         when the caller is not a CREATOR or does not own the project
     * @throws com.creatorconnect.hiring.exception.ProjectNotFoundException
     *         when the project does not exist in the Project Service
     * @throws com.creatorconnect.hiring.exception.DuplicateReviewException
     *         when the freelancer was already reviewed on this project
     * @throws com.creatorconnect.hiring.exception.ReviewValidationException
     *         when the freelancer has no {@code ACCEPTED} application on the
     *         project (they were never hired)
     */
    ReviewResponse createReview(UUID creatorId, String role, ReviewRequest request);

    /**
     * Returns every review received by the freelancer (newest first) together
     * with the average rating across them.
     *
     * @param freelancerId the reviewed freelancer's id
     * @return the aggregated review summary (empty list and {@code 0.0}
     *         average when the freelancer has no reviews yet)
     */
    FreelancerReviewsResponse getFreelancerReviews(UUID freelancerId);
}
