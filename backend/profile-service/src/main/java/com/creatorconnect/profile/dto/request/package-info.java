/**
 * Request DTOs consumed by the Profile Service REST API.
 *
 * <p>{@code ProfileRequest} is the create payload, {@code UpdateProfileRequest}
 * the partial-update payload. Each carries Jakarta Bean Validation annotations
 * so invalid payloads are rejected before reaching the service layer. DTOs are
 * plain data carriers — they are never JPA entities.
 */
package com.creatorconnect.profile.dto.request;
