package com.creatorconnect.hiring.feign;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Minimal user projection returned by the Auth Service user-lookup endpoint.
 *
 * <p>Carries only the fields needed for email notifications: email address
 * and display name components.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserInfoResponse {

    private UUID userId;
    private String firstName;
    private String lastName;
    private String email;
}
