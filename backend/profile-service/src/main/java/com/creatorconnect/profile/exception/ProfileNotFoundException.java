package com.creatorconnect.profile.exception;

/**
 * Thrown when a requested profile does not exist for the given user.
 *
 * <p>Translated to {@code 404 NOT_FOUND} by {@link GlobalExceptionHandler}.
 */
public class ProfileNotFoundException extends RuntimeException {

    /**
     * Creates the exception with a descriptive message.
     *
     * @param message details about the missing profile
     */
    public ProfileNotFoundException(String message) {
        super(message);
    }
}
