package com.creatorconnect.project.dto.response;

import com.creatorconnect.project.entity.ProjectStatus;
import com.creatorconnect.project.feign.ProfileResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Response payload returned by every project endpoint on success.
 *
 * <p>Contains a full projection of the persisted {@code Project} — a pure data
 * carrier with no JPA entity exposure. On read endpoints
 * ({@code GET /projects}, {@code GET /projects/{id}}, {@code GET /projects/my})
 * the service also attaches the owner's public profile as {@link #ownerProfile}
 * (fetched from the Profile Service via OpenFeign). The field is omitted from
 * the JSON when it is {@code null} (the service's Jackson config uses
 * {@code default-property-inclusion: non_null}) — which is the graceful
 * degradation when the owner has no profile yet or the Profile Service is
 * unavailable.
 */
@Getter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class ProjectResponse {

    private UUID id;

    private UUID userId;

    /**
     * The project owner's public profile, attached on read endpoints only.
     *
     * <p>Best-effort: {@code null} (and therefore omitted from the JSON) when
     * the owner has no profile or the Profile Service could not be reached.
     */
    private ProfileResponse ownerProfile;

    private String title;

    private String description;

    private String category;

    private List<String> skillsRequired;

    private BigDecimal budget;

    private String duration;

    private String experienceLevel;

    private String location;

    private ProjectStatus status;

    private LocalDate applicationDeadline;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
