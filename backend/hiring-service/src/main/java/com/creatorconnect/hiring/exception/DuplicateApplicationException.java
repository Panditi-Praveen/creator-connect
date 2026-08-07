package com.creatorconnect.hiring.exception;

/**
 * Thrown when a freelancer tries to apply to a project they already applied
 * to.
 *
 * <p>Translated to {@code 409 CONFLICT} by {@link GlobalExceptionHandler} — a
 * freelancer may submit at most one application per project (enforced by the
 * {@code unique} constraint on {@code applications(project_id, freelancer_id)}
 * at the database level as well).
 */
public class DuplicateApplicationException extends RuntimeException {

    /**
     * Creates the exception with a descriptive message.
     *
     * @param message details about the conflicting application
     */
    public DuplicateApplicationException(String message) {
        super(message);
    }
}
