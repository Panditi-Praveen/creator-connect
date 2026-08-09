package com.creatorconnect.hiring.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response payload returned by the review endpoints on success.
 *
 * <p>Contains a full projection of the persisted {@code Review} — a pure data
 * carrier with no JPA entity exposure.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewResponse {

    private UUID id;

    private UUID projectId;

    private UUID creatorId;

    private UUID freelancerId;

    private Integer rating;

    private String reviewText;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
