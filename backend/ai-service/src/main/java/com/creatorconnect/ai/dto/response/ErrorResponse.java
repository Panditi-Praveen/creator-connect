package com.creatorconnect.ai.dto.response;

import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;

/**
 * Standard error envelope produced by {@code GlobalExceptionHandler} for every
 * failed request.
 *
 * <p>Guarantees the same JSON contract used across the platform:
 * <pre>
 * {
 *   "timestamp": "2026-08-17T12:00:00",
 *   "status": 503,
 *   "error": "Service Unavailable",
 *   "message": "...",
 *   "path": "/ai/discover"
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
    private final Integer retryAfter;

    private ErrorResponse(LocalDateTime timestamp, int status, String error, String message, String path, Integer retryAfter) {
        this.timestamp = timestamp;
        this.status = status;
        this.error = error;
        this.message = message;
        this.path = path;
        this.retryAfter = retryAfter;
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
                path,
                null
        );
    }

    /**
     * Builds a rate-limit {@link ErrorResponse} that includes a retry-after hint.
     *
     * @param message    the human-readable error message
     * @param path       the request URI that failed
     * @param retryAfter seconds the client should wait before retrying (may be {@code null})
     * @return a populated {@link ErrorResponse}
     */
    public static ErrorResponse ofRateLimit(String message, String path, Integer retryAfter) {
        return new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.TOO_MANY_REQUESTS.value(),
                HttpStatus.TOO_MANY_REQUESTS.getReasonPhrase(),
                message,
                path,
                retryAfter
        );
    }
}
