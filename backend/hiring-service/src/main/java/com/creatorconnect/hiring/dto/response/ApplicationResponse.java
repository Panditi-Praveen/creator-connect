package com.creatorconnect.hiring.dto.response;

import com.creatorconnect.hiring.entity.ApplicationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response payload returned by every application endpoint on success.
 *
 * <p>Contains a full projection of the persisted {@code Application} — a pure
 * data carrier with no JPA entity exposure.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationResponse {

    private UUID id;

    private UUID projectId;

    private UUID freelancerId;

    private String proposal;

    private BigDecimal expectedBudget;

    private String estimatedDuration;

    private ApplicationStatus status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
