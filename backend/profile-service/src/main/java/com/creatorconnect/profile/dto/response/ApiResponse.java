package com.creatorconnect.profile.dto.response;

import lombok.Getter;

import java.time.LocalDateTime;

/**
 * Generic envelope wrapping every successful Profile Service response.
 *
 * <p>Provides a consistent JSON shape for clients:
 * <pre>
 * {
 *   "timestamp": "2026-08-03T12:00:00",
 *   "status": 201,
 *   "message": "Profile created successfully",
 *   "data": { ... },
 *   "path": "/profile"
 * }
 * </pre>
 *
 * <p>Error responses use a separate, dedicated {@link ErrorResponse} contract.
 *
 * @param <T> the type of the payload carried in {@code data}
 */
@Getter
public final class ApiResponse<T> {

    private final LocalDateTime timestamp;
    private final int status;
    private final String message;
    private final T data;
    private final String path;

    private ApiResponse(LocalDateTime timestamp, int status, String message, T data, String path) {
        this.timestamp = timestamp;
        this.status = status;
        this.message = message;
        this.data = data;
        this.path = path;
    }

    /**
     * Creates a success response envelope.
     *
     * @param status  the HTTP status code of the operation
     * @param message a human-readable summary of the outcome
     * @param data    the payload (may be {@code null})
     * @param path    the request URI that produced this response
     * @param <T>     the payload type
     * @return a populated {@link ApiResponse}
     */
    public static <T> ApiResponse<T> success(int status, String message, T data, String path) {
        return new ApiResponse<>(LocalDateTime.now(), status, message, data, path);
    }
}
