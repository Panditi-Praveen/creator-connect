package com.creatorconnect.ai.dto.response;

/**
 * Payload returned by {@code GET /ai/status}.
 *
 * @param service the registered service name
 * @param status  the service status ({@code UP} when the application is running)
 */
public record StatusResponse(String service, String status) {
}
