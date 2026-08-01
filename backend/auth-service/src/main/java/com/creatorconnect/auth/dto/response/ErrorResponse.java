package com.creatorconnect.auth.dto.response;

import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;

/**
 * Standard error envelope produced by {@code GlobalExceptionHandler} for every
 * failed request.
 *
 * <p>Guarantees the exact JSON contract:
 * <pre>
 * {
 *   "timestamp": "2026-08-01T12:00:00",
 *   "status": 409,
 *   "error": "Conflict",
 *   "message": "Email is already registered: ...",
 *   "path": "/auth/register"
 * }
 * </pre>
 *
 * <p>Clients can rely on {@code status} and {@code message} for programmatic
 * handling and user-facing display respectively.
 */
@Getter
public final class ErrorResponse {

    private final LocalDateTime timestamp;
    private final int status;
    private final String error;
    private final String message;
    private final String path;

    private ErrorResponse(LocalDateTime timestamp, int status, String error, String message, String path) {
        this.timestamp = timestamp;
        this.status = status;
        this.error = error;
        this.message = message;
        this.path = path;
    }

    /**
     * Builds an {@link ErrorResponse} from an HTTP status and message.
     *
     * @param status  the HTTP status of the failure
     * @param message the human-readable error message
     * @param path    the request URI that failed
     * @return a populated {@link ErrorResponse}
     */
    public static ErrorResponse of(HttpStatus status, String message, String path) {
        return new ErrorResponse(
                LocalDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                path
        );
    }
}
