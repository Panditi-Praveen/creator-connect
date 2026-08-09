package com.creatorconnect.hiring.exception;

/**
 * Thrown when a request is structurally valid but violates a business rule
 * that Jakarta Bean Validation cannot express — e.g. reviewing a freelancer
 * who has no {@code ACCEPTED} application on the project (they were never
 * hired).
 *
 * <p>Translated to {@code 400 BAD_REQUEST} by {@link GlobalExceptionHandler}.
 */
public class ReviewValidationException extends RuntimeException {

    /**
     * Creates the exception with a descriptive message.
     *
     * @param message details about the rule that was violated
     */
    public ReviewValidationException(String message) {
        super(message);
    }
}
