package com.creatorconnect.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request payload for {@code POST /auth/login}.
 *
 * <p>Carries the credentials used to authenticate an existing CreatorConnect
 * account. Validation mirrors the registration contract so a login attempt
 * never reaches the service layer with malformed input:
 * <ul>
 *   <li>Email must be non-blank and well-formed (max 150 chars).</li>
 *   <li>Password must be non-blank and within the allowed length window.</li>
 * </ul>
 *
 * <p>Validation failures are translated into {@code 400 BAD_REQUEST} responses
 * by {@code GlobalExceptionHandler}; bad credentials are rejected later by the
 * service layer with {@code 401 UNAUTHORIZED}.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be a well-formed email address")
    @Size(max = 150, message = "Email must not exceed 150 characters")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 64, message = "Password must be between 8 and 64 characters")
    private String password;
}
