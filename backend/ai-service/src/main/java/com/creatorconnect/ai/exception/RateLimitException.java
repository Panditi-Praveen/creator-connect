package com.creatorconnect.ai.exception;

/**
 * Thrown when the external LLM provider returns HTTP 429 (Too Many Requests).
 *
 * <p>Carries the optional {@code retryAfter} value extracted from the provider's
 * {@code Retry-After} response header so the client can display a countdown,
 * and the {@code errorType} from the provider's JSON error body
 * (e.g. {@code rate_limit_exceeded}, {@code insufficient_quota}).
 */
public class RateLimitException extends RuntimeException {

    private final Integer retryAfter;
    private final String errorType;

    public RateLimitException(String message, Integer retryAfter, String errorType) {
        super(message);
        this.retryAfter = retryAfter;
        this.errorType = errorType;
    }

    /**
     * Returns the number of seconds the client should wait before retrying,
     * or {@code null} if the provider did not specify one.
     */
    public Integer getRetryAfter() {
        return retryAfter;
    }

    /**
     * Returns the provider error type (e.g. {@code rate_limit_exceeded},
     * {@code insufficient_quota}), or {@code null} if not available.
     */
    public String getErrorType() {
        return errorType;
    }
}
