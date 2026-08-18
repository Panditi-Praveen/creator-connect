package com.creatorconnect.auth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Minimal user projection returned by the internal user-lookup endpoint.
 *
 * <p>Only carries the fields needed by other services (e.g. the Hiring
 * Service for email notifications).  Passwords and other sensitive data
 * are never exposed.
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
