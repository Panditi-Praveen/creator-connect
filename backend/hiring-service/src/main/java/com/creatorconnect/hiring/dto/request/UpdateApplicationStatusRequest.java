package com.creatorconnect.hiring.dto.request;

import com.creatorconnect.hiring.entity.ApplicationStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request payload for {@code PUT /applications/{id}/status}.
 *
 * <p>Carries the new status a creator assigns to an application. Only
 * {@code ACCEPTED} and {@code REJECTED} are valid creator decisions —
 * {@code PENDING} (the default at creation) and {@code WITHDRAWN} (set by the
 * freelancer) are not reachable through this endpoint and are rejected with
 * {@code 400 BAD_REQUEST} by the service layer.
 *
 * <p>Validation failures are translated into {@code 400 BAD_REQUEST} responses
 * by {@code GlobalExceptionHandler}.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateApplicationStatusRequest {

    @NotNull(message = "Status is required")
    private ApplicationStatus status;
}
