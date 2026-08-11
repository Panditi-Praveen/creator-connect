package com.creatorconnect.hiring.feign;

/**
 * Client-side mirror of the Project Service's {@code ProjectStatus} lifecycle
 * enum, deserialized from {@code GET /projects/{id}} and used to drive the
 * Hiring Service's project-workflow rules.
 *
 * <p>Keeping a mirror enum here (instead of depending on the Project Service
 * module) preserves the Hiring Service's independence — the two services only
 * share JSON over the wire.
 */
public enum ProjectStatus {

    /**
     * The project is published and accepting applications.
     */
    OPEN,

    /**
     * A creator has been selected and work is underway (set automatically when
     * the creator accepts an application).
     */
    IN_PROGRESS,

    /**
     * The project has finished (only completed projects can be reviewed).
     */
    COMPLETED,

    /**
     * The project was withdrawn before completion.
     */
    CANCELLED
}
