package com.creatorconnect.hiring.feign;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Client-side request payload mirroring the Project Service's
 * {@code UpdateProjectStatusRequest}, sent on
 * {@code PUT /projects/{id}/status}.
 *
 * <p>Lombok generates a no-args constructor (needed by Jackson to serialize
 * the Feign request body) plus the all-args constructor used by the service
 * layer. Only the new status is carried — the Project Service applies and
 * validates its own lifecycle state machine.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class UpdateProjectStatusRequest {

    private ProjectStatus status;
}
