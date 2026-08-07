package com.creatorconnect.hiring.exception;

/**
 * Thrown when an authenticated user attempts an operation they are not
 * allowed to perform: a non-freelancer applying to a project, a non-creator
 * reading or deciding on applications, or a freelancer withdrawing an
 * application they do not own.
 *
 * <p>Translated to {@code 403 FORBIDDEN} by {@link GlobalExceptionHandler}.
 * Ownership is always derived from the JWT's {@code userId} / {@code role}
 * claims — a caller can only ever act within their own role and on their own
 * applications.
 */
public class ApplicationAccessDeniedException extends RuntimeException {

    /**
     * Creates the exception with a descriptive message.
     *
     * @param message details about the denied operation
     */
    public ApplicationAccessDeniedException(String message) {
        super(message);
    }
}
