package com.creatorconnect.project.exception;

/**
 * Thrown when an authenticated user attempts to update or delete a project
 * they do not own.
 *
 * <p>Translated to {@code 403 FORBIDDEN} by {@link GlobalExceptionHandler}.
 * Ownership is always derived from the JWT's {@code userId} claim — a caller
 * can only ever modify their own projects.
 */
public class ProjectAccessDeniedException extends RuntimeException {

    /**
     * Creates the exception with a descriptive message.
     *
     * @param message details about the denied operation
     */
    public ProjectAccessDeniedException(String message) {
        super(message);
    }
}
