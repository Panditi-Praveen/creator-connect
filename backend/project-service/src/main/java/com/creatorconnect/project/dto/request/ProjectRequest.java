package com.creatorconnect.project.dto.request;

import com.creatorconnect.project.entity.ProjectStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Request payload for {@code POST /projects}.
 *
 * <p>Carries the details of a creative project a user wants to post.
 * {@code title}, {@code description}, {@code category}, {@code budget},
 * {@code duration}, {@code experienceLevel} and {@code applicationDeadline}
 * are mandatory; {@code skillsRequired}, {@code location} and {@code status}
 * are optional (an omitted {@code status} defaults to {@code OPEN}).
 *
 * <p>The owning {@code userId} is deliberately <em>not</em> part of this DTO —
 * it is taken from the authenticated JWT so a caller can never post a project
 * on someone else's behalf.
 *
 * <p>Validation failures are translated into {@code 400 BAD_REQUEST} responses
 * by {@code GlobalExceptionHandler}.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 200, message = "Title must not exceed 200 characters")
    private String title;

    @NotBlank(message = "Description is required")
    @Size(max = 5000, message = "Description must not exceed 5000 characters")
    private String description;

    @NotBlank(message = "Category is required")
    @Size(max = 100, message = "Category must not exceed 100 characters")
    private String category;

    /**
     * Required skills — optional, each entry trimmed on save.
     */
    @Size(max = 50, message = "At most 50 skills are allowed")
    private List<@NotBlank(message = "Skills must not be blank")
            @Size(max = 100, message = "Each skill must not exceed 100 characters") String> skillsRequired;

    @NotNull(message = "Budget is required")
    @DecimalMin(value = "0.0", message = "Budget must be at least 0")
    @Digits(integer = 10, fraction = 2, message = "Budget must have at most 10 integer digits and 2 decimal places")
    private BigDecimal budget;

    @NotBlank(message = "Duration is required")
    @Size(max = 50, message = "Duration must not exceed 50 characters")
    private String duration;

    @NotBlank(message = "Experience level is required")
    @Size(max = 50, message = "Experience level must not exceed 50 characters")
    private String experienceLevel;

    @Size(max = 100, message = "Location must not exceed 100 characters")
    private String location;

    /**
     * Optional — defaults to {@code OPEN} when omitted.
     */
    private ProjectStatus status;

    @NotNull(message = "Application deadline is required")
    @Future(message = "Application deadline must be in the future")
    private LocalDate applicationDeadline;
}
