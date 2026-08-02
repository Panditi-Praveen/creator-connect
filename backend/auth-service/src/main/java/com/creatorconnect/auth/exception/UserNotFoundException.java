package com.creatorconnect.auth.exception;

/**
 * Thrown when a login attempt references an email address that does not match
 * any registered account.
 *
 * <p>Translated to {@code 401 UNAUTHORIZED} by {@link GlobalExceptionHandler}
 * so login failures never leak whether an email is registered.
 */
public class UserNotFoundException extends RuntimeException {

    /**
     * Creates the exception with a descriptive message.
     *
     * @param message details about the missing account
     */
    public UserNotFoundException(String message) {
        super(message);
    }
}
