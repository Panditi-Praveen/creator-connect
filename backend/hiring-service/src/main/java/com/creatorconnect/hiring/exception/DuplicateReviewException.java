package com.creatorconnect.hiring.exception;

/**
 * Thrown when a creator tries to review a freelancer on a project they were
 * already reviewed on.
 *
 * <p>Translated to {@code 409 CONFLICT} by {@link GlobalExceptionHandler} — a
 * freelancer may receive at most one review per project (enforced by the
 * {@code unique} constraint on {@code reviews(project_id, freelancer_id)} at
 * the database level as well).
 */
public class DuplicateReviewException extends RuntimeException {

    /**
     * Creates the exception with a descriptive message.
     *
     * @param message details about the conflicting review
     */
    public DuplicateReviewException(String message) {
        super(message);
    }
}
