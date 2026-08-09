package com.creatorconnect.hiring.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Response payload for {@code GET /reviews/freelancer/{freelancerId}}.
 *
 * <p>Aggregates a freelancer's reviews (newest first) together with the
 * average rating across them — the reputation signal creators see when
 * evaluating talent. When the freelancer has no reviews yet, {@code reviews}
 * is empty, {@code totalReviews} is {@code 0} and {@code averageRating} is
 * {@code 0.0}.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FreelancerReviewsResponse {

    private UUID freelancerId;

    /**
     * Arithmetic mean of the review ratings, rounded to one decimal place
     * ({@code 0.0} when there are no reviews).
     */
    private double averageRating;

    private int totalReviews;

    private List<ReviewResponse> reviews;
}
