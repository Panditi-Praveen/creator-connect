package com.creatorconnect.hiring.exception;

/**
 * Thrown when a request is structurally valid but violates a business rule
 * that Jakarta Bean Validation cannot express — e.g. requesting a status other
 * than {@code ACCEPTED} / {@code REJECTED} on the creator decision endpoint.
 *
 * <p>Translated to {@code 400 BAD_REQUEST} by {@link GlobalExceptionHandler}.
 */
public class ApplicationValidationException extends RuntimeException {

    /**
     * Creates the exception with a descriptive message.
     *
     * @param message details about the rule that was violated
     */
    public ApplicationValidationException(String message) {
        super(message);
    }
}
