package com.creatorconnect.profile.exception;

/**
 * Thrown when a user who already owns a profile tries to create another one.
 *
 * <p>Translated to {@code 409 CONFLICT} by {@link GlobalExceptionHandler} —
 * a user may have exactly one profile (enforced by the {@code unique}
 * constraint on {@code profiles.user_id} as well).
 */
public class ProfileAlreadyExistsException extends RuntimeException {

    /**
     * Creates the exception with a descriptive message.
     *
     * @param message details about the conflicting profile
     */
    public ProfileAlreadyExistsException(String message) {
        super(message);
    }
}
