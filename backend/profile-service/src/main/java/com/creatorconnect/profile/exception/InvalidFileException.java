package com.creatorconnect.profile.exception;

/**
 * Thrown when an uploaded file fails validation (wrong type, too large, etc.).
 *
 * <p>Translated into {@code 400 BAD_REQUEST} by {@link com.creatorconnect.profile.exception.GlobalExceptionHandler}.
 */
public class InvalidFileException extends RuntimeException {

    public InvalidFileException(String message) {
        super(message);
    }
}
