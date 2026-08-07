package com.creatorconnect.project.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Optional query-parameter filters for the project feed.
 *
 * <p>Bound from the query string of {@code GET /projects} and
 * {@code GET /projects/my} via {@code @ModelAttribute}. Every field is
 * optional ({@code null} = that filter is skipped) and any combination may be
 * used at once; all active filters are ANDed together. Filtering happens in
 * the database — a single parameterized query, no feed data is loaded into
 * memory.
 *
 * <p>Supported filters:
 * <ul>
 *   <li>{@code category} — exact match (case-insensitive)</li>
 *   <li>{@code skill} — projects whose required skills contain this skill
 *       (substring match, case-insensitive)</li>
 *   <li>{@code budgetMin} / {@code budgetMax} — inclusive budget range</li>
 *   <li>{@code experienceLevel} — exact match (case-insensitive)</li>
 *   <li>{@code location} — exact match (case-insensitive)</li>
 *   <li>{@code keyword} — substring match on title or description
 *       (case-insensitive)</li>
 * </ul>
 *
 * <p>Malformed values (e.g. {@code budgetMin=abc}) or values that violate the
 * constraints below are rejected with {@code 400 BAD_REQUEST} by
 * {@code GlobalExceptionHandler}. Empty or whitespace-only string values are
 * treated as "no filter" (normalized to {@code null} by the service layer),
 * so a frontend can safely send {@code ?category=} when a filter is cleared.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectFilter {

    @Schema(description = "Exact category match, case-insensitive", example = "Video Editing")
    @Size(max = 100, message = "Category filter must not exceed 100 characters")
    private String category;

    @Schema(description = "A skill the project must require (substring match)", example = "After Effects")
    @Size(max = 100, message = "Skill filter must not exceed 100 characters")
    private String skill;

    @Schema(description = "Minimum budget, inclusive", example = "100")
    @DecimalMin(value = "0.0", message = "Budget minimum must be at least 0")
    @Digits(integer = 10, fraction = 2, message = "Budget minimum must have at most 10 integer digits and 2 decimal places")
    private BigDecimal budgetMin;

    @Schema(description = "Maximum budget, inclusive", example = "1000")
    @DecimalMin(value = "0.0", message = "Budget maximum must be at least 0")
    @Digits(integer = 10, fraction = 2, message = "Budget maximum must have at most 10 integer digits and 2 decimal places")
    private BigDecimal budgetMax;

    @Schema(description = "Exact experience level match, case-insensitive", example = "Intermediate")
    @Size(max = 50, message = "Experience level filter must not exceed 50 characters")
    private String experienceLevel;

    @Schema(description = "Exact location match, case-insensitive", example = "Remote")
    @Size(max = 100, message = "Location filter must not exceed 100 characters")
    private String location;

    @Schema(description = "Substring match on title or description, case-insensitive", example = "youtube")
    @Size(max = 200, message = "Keyword filter must not exceed 200 characters")
    private String keyword;
}
