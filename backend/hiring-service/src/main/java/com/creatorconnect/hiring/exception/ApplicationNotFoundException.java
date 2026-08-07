package com.creatorconnect.hiring.exception;

/**
 * Thrown when a requested application does not exist.
 *
 * <p>Translated to {@code 404 NOT_FOUND} by {@link GlobalExceptionHandler}.
 */
public class ApplicationNotFoundException extends RuntimeException {

    /**
     * Creates the exception with a descriptive message.
     *
     * @param message details about the missing application
     */
    public ApplicationNotFoundException(String message) {
        super(message);
    }
}
