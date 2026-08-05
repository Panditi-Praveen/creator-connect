package com.creatorconnect.project.exception;

/**
 * Thrown when a requested project does not exist.
 *
 * <p>Translated to {@code 404 NOT_FOUND} by {@link GlobalExceptionHandler}.
 */
public class ProjectNotFoundException extends RuntimeException {

    /**
     * Creates the exception with a descriptive message.
     *
     * @param message details about the missing project
     */
    public ProjectNotFoundException(String message) {
        super(message);
    }
}
