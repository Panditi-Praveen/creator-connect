package com.creatorconnect.ai.feign;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Client-side projection of the Profile Service's {@code ProfileResponse}
 * JSON payload (mirrors the exact field set served by {@code GET
 * /profile/freelancers}).
 *
 * <p>Needed only to deserialize Feign responses; the AI Service never exposes
 * this DTO directly to API clients (results are converted into
 * {@code TalentResult}).
 */
@Getter
@Setter
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
