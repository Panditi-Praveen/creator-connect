package com.creatorconnect.ai.exception;

/**
 * Thrown when the Profile Service cannot be reached or answers an error
 * status during discovery. Mapped to {@code 503 SERVICE_UNAVAILABLE} by the
 * global exception handler with a generic, client-safe message.
 */
public class ProfileServiceUnavailableException extends RuntimeException {

    /**
     * Creates the exception with a client-safe message.
     *
     * @param message the client-safe summary
     */
    public ProfileServiceUnavailableException(String message) {
        super(message);
    }
}
