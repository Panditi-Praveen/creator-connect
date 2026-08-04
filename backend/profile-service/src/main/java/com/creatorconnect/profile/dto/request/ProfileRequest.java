package com.creatorconnect.profile.dto.request;

import com.creatorconnect.profile.util.ValidUrl;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request payload for {@code POST /profile}.
 *
 * <p>Carries the public professional data of a creator/freelancer profile.
 * Only {@code firstName} and {@code lastName} are mandatory; everything else
 * is optional and defaults to {@code null} (with {@code availableForHire}
 * defaulting to {@code true}).
 *
 * <p>The owning {@code userId} is deliberately <em>not</em> part of this DTO —
 * it is taken from the authenticated JWT so a caller can never create a
 * profile for someone else.
 *
 * <p>Validation failures are translated into {@code 400 BAD_REQUEST} responses
 * by {@code GlobalExceptionHandler}.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileRequest {

    @NotBlank(message = "First name is required")
    @Size(max = 50, message = "First name must not exceed 50 characters")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(max = 50, message = "Last name must not exceed 50 characters")
    private String lastName;

    @Size(max = 150, message = "Headline must not exceed 150 characters")
    private String headline;

    @Size(max = 2000, message = "Bio must not exceed 2000 characters")
    private String bio;

    @ValidUrl
    @Size(max = 500, message = "Profile image URL must not exceed 500 characters")
    private String profileImageUrl;

    @Size(max = 100, message = "Location must not exceed 100 characters")
    private String location;

    @ValidUrl
    @Size(max = 300, message = "Website must not exceed 300 characters")
    private String website;

    @ValidUrl
    @Size(max = 300, message = "LinkedIn URL must not exceed 300 characters")
    private String linkedin;

    @ValidUrl
    @Size(max = 300, message = "GitHub URL must not exceed 300 characters")
    private String github;

    @Size(max = 1000, message = "Skills must not exceed 1000 characters")
    private String skills;

    @Min(value = 0, message = "Experience must be at least 0")
    @Max(value = 100, message = "Experience must not exceed 100")
    private Integer experience;

    /**
     * Optional — defaults to {@code true} when omitted.
     */
    private Boolean availableForHire;
}
