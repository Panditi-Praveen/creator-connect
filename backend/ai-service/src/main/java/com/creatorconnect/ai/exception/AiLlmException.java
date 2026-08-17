package com.creatorconnect.ai.exception;

/**
 * Thrown when the external LLM call fails (network error, error status from
 * the provider, or an unparseable response). Mapped to
 * {@code 502 BAD_GATEWAY} by the global exception handler with a generic,
 * client-safe message.
 */
public class AiLlmException extends RuntimeException {

    /**
     * Creates the exception with a generic client-safe message.
     *
     * @param message the client-safe summary
     * @param cause   the underlying failure (logged server-side)
     */
    public AiLlmException(String message, Throwable cause) {
        super(message, cause);
    }
}
