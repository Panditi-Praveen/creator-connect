package com.creatorconnect.hiring.exception;

/**
 * Thrown when a referenced project does not exist in the Project Service.
 *
 * <p>Raised by {@code ProjectClientService} when the Project Service answers
 * {@code 404} for {@code GET /projects/{id}}. Translated to
 * {@code 404 NOT_FOUND} by {@link GlobalExceptionHandler}.
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
