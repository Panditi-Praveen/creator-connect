package com.creatorconnect.auth.dto.response;

import com.creatorconnect.auth.entity.AuthProvider;
import com.creatorconnect.auth.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response payload returned by {@code POST /auth/register}.
 *
 * <p>Contains a safe projection of the persisted {@code User} — sensitive
 * fields such as the BCrypt password hash are never exposed.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterResponse {

    private UUID id;

    private String firstName;

    private String lastName;

    private String email;

    private String phone;

    private Role role;

    private AuthProvider provider;

    private LocalDateTime createdAt;
}
