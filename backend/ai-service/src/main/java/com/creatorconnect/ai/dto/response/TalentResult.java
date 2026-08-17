package com.creatorconnect.ai.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * One ranked talent match returned by {@code POST /ai/discover}.
 *
 * <p>All fields come from the real {@code Profile} model served by the
 * Profile Service ({@code GET /profile/freelancers}); the {@code score} and
 * {@code reason} are produced by the LLM-assisted ranking step. {@code skills}
 * is a single string exactly as stored on the profile (comma-separated by
 * convention of the Profile Service).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TalentResult {

    /**
     * The profile's own id (Profile Service {@code profiles.id}).
     */
    private UUID profileId;

    /**
     * The owning user's id (Profile Service {@code profiles.user_id}, the
     * platform-wide identity used by other services).
     */
    private UUID userId;

    /**
     * Display name ({@code firstName + lastName}).
     */
    private String name;

    private String headline;

    private String skills;

    private String location;

    private Boolean availableForHire;

    /**
     * LLM-computed relevance score in {@code [0.0, 1.0]}.
     */
    private Double score;

    /**
     * Short human-readable justification for the match.
     */
    private String reason;
}
