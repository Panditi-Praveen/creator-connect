package com.creatorconnect.project.dto.response;

import com.creatorconnect.project.entity.ProjectStatus;
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
 * carrier with no JPA entity exposure.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectResponse {

    private UUID id;

    private UUID userId;

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
