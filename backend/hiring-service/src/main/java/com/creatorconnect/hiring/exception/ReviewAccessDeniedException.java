package com.creatorconnect.hiring.exception;

/**
 * Thrown when an authenticated user attempts a review operation they are not
 * allowed to perform — e.g. a non-creator submitting a review.
 *
 * <p>Translated to {@code 403 FORBIDDEN} by {@link GlobalExceptionHandler}.
 * The creator identity is always derived from the JWT's {@code userId} /
 * {@code role} claims — a caller can only ever act within their own role.
 */
public class ReviewAccessDeniedException extends RuntimeException {

    /**
     * Creates the exception with a descriptive message.
     *
     * @param message details about the denied operation
     */
    public ReviewAccessDeniedException(String message) {
        super(message);
    }
}
