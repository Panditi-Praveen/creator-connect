package com.creatorconnect.project.feign;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * Client-side projection of the Profile Service's {@code ProfileResponse}
 * payload, deserialized from {@code GET /profile/{userId}}.
 *
 * <p>Only the fields the Project Service attaches to project reads are
 * declared — {@code userId} (to tie the profile to the project owner),
 * {@code firstName}/{@code lastName} (the owner's name), {@code headline},
 * {@code profileImageUrl} and {@code skills} (the public "about the owner"
 * summary shown next to a project). Unknown fields returned by the Profile
 * Service are ignored by Jackson (Spring Boot disables
 * {@code FAIL_ON_UNKNOWN_PROPERTIES} by default). This DTO is deliberately
 * independent of the Profile Service module — the Project Service must never
 * depend on another service's classes.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProfileResponse {

    private UUID userId;

    private String firstName;

    private String lastName;

    private String headline;

    private String profileImageUrl;

    private String skills;
}
