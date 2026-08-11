package com.creatorconnect.project.dto.request;

import com.creatorconnect.project.entity.ProjectStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request payload for {@code PUT /projects/{id}/status}.
 *
 * <p>Carries only the new lifecycle state of the project. Unlike the general
 * {@link UpdateProjectRequest} (which lets an owner set the status to
 * anything), this dedicated endpoint applies the project state machine: only
 * forward transitions are allowed ({@code OPEN &rarr; IN_PROGRESS /
 * COMPLETED / CANCELLED}, {@code IN_PROGRESS &rarr; COMPLETED / CANCELLED}),
 * terminal states ({@code COMPLETED}, {@code CANCELLED}) are locked, and
 * re-applying the current status is an idempotent no-op. Illegal transitions
 * are rejected with {@code 409 CONFLICT}.
 *
 * <p>This is the endpoint the Hiring Service calls (via OpenFeign) when a
 * creator accepts an application, moving the project to
 * {@code IN_PROGRESS} automatically.
 *
 * <p>Validation failures are translated into {@code 400 BAD_REQUEST} responses
 * by {@code GlobalExceptionHandler}.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProjectStatusRequest {

    @NotNull(message = "Status is required")
    private ProjectStatus status;
}
