package com.creatorconnect.auth.dto.response;

import com.creatorconnect.auth.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Response payload returned by {@code POST /auth/login} on success.
 *
 * <p>Carries the freshly issued JWT plus a safe projection of the
 * authenticated user (the BCrypt hash is never exposed). The
 * {@code expiresIn} value mirrors the configured token lifetime in seconds.
 *
 * <p>Example JSON:
 * <pre>
 * {
 *   "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
 *   "tokenType": "Bearer",
 *   "expiresIn": 86400,
 *   "userId": "5c2f...-uuid",
 *   "email": "praveen@gmail.com",
 *   "role": "CREATOR"
 * }
 * </pre>
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {

    private String accessToken;

    /**
     * Token scheme — always {@code Bearer} for JWT.
     */
    @Builder.Default
    private String tokenType = "Bearer";

    /**
     * Token lifetime in seconds, derived from the configured JWT expiration.
     */
    private long expiresIn;

    private UUID userId;

    private String email;

    private Role role;
}
