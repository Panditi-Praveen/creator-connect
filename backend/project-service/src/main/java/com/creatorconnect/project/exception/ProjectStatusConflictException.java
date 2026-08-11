package com.creatorconnect.project.exception;

/**
 * Thrown when a status transition would move a project against its lifecycle
 * state machine: re-opening an {@code IN_PROGRESS} project, or changing a
 * project in a terminal state ({@code COMPLETED}, {@code CANCELLED}).
 *
 * <p>Translated to {@code 409 CONFLICT} by {@link GlobalExceptionHandler} —
 * the requested transition conflicts with the project's current state.
 */
public class ProjectStatusConflictException extends RuntimeException {

    /**
     * Creates the exception with a descriptive message.
     *
     * @param message details about the illegal transition
     */
    public ProjectStatusConflictException(String message) {
        super(message);
    }
}
