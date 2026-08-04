package com.creatorconnect.profile.exception;

/**
 * Thrown when an authenticated user attempts to update or delete a profile
 * they do not own.
 *
 * <p>Translated to {@code 403 FORBIDDEN} by {@link GlobalExceptionHandler}.
 * Ownership is always derived from the JWT's {@code userId} claim — a caller
 * can only ever modify their own profile.
 */
public class ProfileAccessDeniedException extends RuntimeException {

    /**
     * Creates the exception with a descriptive message.
     *
     * @param message details about the denied operation
     */
    public ProfileAccessDeniedException(String message) {
        super(message);
    }
}
