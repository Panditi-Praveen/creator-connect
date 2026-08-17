package com.creatorconnect.ai.exception;

/**
 * Thrown when the AI service is not configured (e.g. the
 * {@code OPENAI_API_KEY} environment variable is missing). Mapped to
 * {@code 503 SERVICE_UNAVAILABLE} by the global exception handler. The
 * message is deliberately safe to expose to clients — it never contains
 * secrets.
 */
public class AiConfigurationException extends RuntimeException {

    /**
     * Creates the exception with a client-safe message.
     *
     * @param message the reason the AI service cannot run
     */
    public AiConfigurationException(String message) {
        super(message);
    }
}
