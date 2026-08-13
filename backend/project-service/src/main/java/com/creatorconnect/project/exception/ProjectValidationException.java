package com.creatorconnect.project.exception;

/**
 * Thrown when a request is structurally valid but violates a business rule
 * that Jakarta Bean Validation cannot express — e.g. creating a project
 * directly in a non-{@code OPEN} state. A project is created {@code OPEN} by
 * design and must move through the lifecycle state machine; requesting a
 * different initial state is rejected here.
 *
 * <p>Translated to {@code 400 BAD_REQUEST} by {@link GlobalExceptionHandler} —
 * the same convention the Hiring Service uses for its application/review
 * business rules ({@code ApplicationValidationException} / {@code
 * ReviewValidationException}).
 */
public class ProjectValidationException extends RuntimeException {

    /**
     * Creates the exception with a descriptive message.
     *
     * @param message details about the rule that was violated
     */
    public ProjectValidationException(String message) {
        super(message);
    }
}
