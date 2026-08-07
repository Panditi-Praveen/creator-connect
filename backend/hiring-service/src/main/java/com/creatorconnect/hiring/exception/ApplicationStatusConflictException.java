package com.creatorconnect.hiring.exception;

/**
 * Thrown when an operation would move an application through an illegal status
 * transition: deciding on or withdrawing an application that is no longer
 * {@code PENDING} (already accepted, rejected, or withdrawn).
 *
 * <p>Translated to {@code 409 CONFLICT} by {@link GlobalExceptionHandler} —
 * the requested transition conflicts with the application's current state.
 */
public class ApplicationStatusConflictException extends RuntimeException {

    /**
     * Creates the exception with a descriptive message.
     *
     * @param message details about the illegal transition
     */
    public ApplicationStatusConflictException(String message) {
        super(message);
    }
}
