package com.creatorconnect.auth.exception;

/**
 * Thrown when a registration attempt uses an email address that is already
 * registered.
 *
 * <p>Translated to {@code 409 CONFLICT} by {@link GlobalExceptionHandler}.
 */
public class EmailAlreadyExistsException extends RuntimeException {

    /**
     * Creates the exception with a descriptive message.
     *
     * @param message details about the conflicting email
     */
    public EmailAlreadyExistsException(String message) {
        super(message);
    }
}
