package com.creatorconnect.hiring.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * Request payload for {@code POST /reviews}.
 *
 * <p>Carries everything a creator submits when reviewing a freelancer:
 * {@code projectId}, {@code freelancerId} and {@code rating} (1–5) are
 * mandatory; {@code reviewText} is optional (max 2000 characters).
 *
 * <p>The {@code creatorId} is deliberately <em>not</em> part of this DTO — it
 * is taken from the authenticated JWT so a caller can never review on someone
 * else's behalf. Who may review whom (creator role, hired freelancer, one
 * review per project/freelancer pair) is enforced by the service layer.
 *
 * <p>Validation failures are translated into {@code 400 BAD_REQUEST} responses
 * by {@code GlobalExceptionHandler}.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewRequest {

    @NotNull(message = "Project id is required")
    private UUID projectId;

    @NotNull(message = "Freelancer id is required")
    private UUID freelancerId;

    @NotNull(message = "Rating is required")
    @Min(value = 1, message = "Rating must be between 1 and 5")
    @Max(value = 5, message = "Rating must be between 1 and 5")
    private Integer rating;

    @Size(max = 2000, message = "Review text must not exceed 2000 characters")
    private String reviewText;
}
