package com.creatorconnect.auth.exception;

/**
 * Thrown when authentication fails because the supplied credentials are
 * invalid — either the password does not match or the account is disabled.
 *
 * <p>Translated to {@code 401 UNAUTHORIZED} by {@link GlobalExceptionHandler}.
 */
public class InvalidCredentialsException extends RuntimeException {

    /**
     * Creates the exception with a descriptive message.
     *
     * @param message details about the failed credential check
     */
    public InvalidCredentialsException(String message) {
        super(message);
    }
}
