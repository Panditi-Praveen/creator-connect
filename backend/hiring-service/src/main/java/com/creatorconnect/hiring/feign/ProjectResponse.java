package com.creatorconnect.hiring.feign;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * Client-side projection of the Project Service's {@code ProjectResponse}
 * payload, deserialized from {@code GET /projects/{id}}.
 *
 * <p>Only the fields the Hiring Service needs are declared — {@code id} (to
 * prove the project exists), {@code userId} (the project owner, used for
 * creator ownership checks) and {@code status} (the project's lifecycle
 * state, used to drive the hiring workflow: accepting an application moves a
 * project to {@code IN_PROGRESS}, and only {@code COMPLETED} projects can be
 * reviewed). Unknown fields returned by the Project Service are ignored by
 * Jackson (Spring Boot disables {@code FAIL_ON_UNKNOWN_PROPERTIES} by
 * default). This DTO is deliberately independent of the Project Service module
 * — the Hiring Service must never depend on another service's classes.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProjectResponse {

    private UUID id;

    /**
     * The project owner (matches the {@code userId} JWT claim of the creator
     * who posted the project in the Project Service).
     */
    private UUID userId;

    /**
     * The project's current lifecycle state.
     */
    private ProjectStatus status;
}
