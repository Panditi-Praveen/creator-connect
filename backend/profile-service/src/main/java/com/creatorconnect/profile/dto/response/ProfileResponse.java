package com.creatorconnect.profile.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response payload returned by every profile endpoint on success.
 *
 * <p>Contains a full projection of the persisted {@code Profile} — a pure data
 * carrier with no JPA entity exposure.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileResponse {

    private UUID id;

    private UUID userId;

    private String firstName;

    private String lastName;

    private String headline;

    private String bio;

    private String profileImageUrl;

    private String location;

    private String website;

    private String linkedin;

    private String github;

    private String skills;

    private Integer experience;

    private Boolean availableForHire;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
