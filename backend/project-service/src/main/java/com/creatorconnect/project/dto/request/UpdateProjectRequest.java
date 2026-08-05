package com.creatorconnect.project.dto.request;

import com.creatorconnect.project.entity.ProjectStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
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
 * Request payload for {@code PUT /projects/{id}}.
 *
 * <p>Deliberately <em>partial-update</em> (PUT-as-PATCH) semantics: every
 * field is optional, and a {@code null} field leaves the stored value
 * untouched. This lets clients send only the fields they want to change
 * without clobbering the rest of the project.
 *
 * <p>Constraints mirror {@link ProjectRequest} minus the {@code @NotBlank} /
 * {@code @NotNull} requirements — an update never requires re-sending the
 * mandatory fields.
 *
 * <p>Validation failures are translated into {@code 400 BAD_REQUEST} responses
 * by {@code GlobalExceptionHandler}.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProjectRequest {

    @Size(max = 200, message = "Title must not exceed 200 characters")
    private String title;

    @Size(max = 5000, message = "Description must not exceed 5000 characters")
    private String description;

    @Size(max = 100, message = "Category must not exceed 100 characters")
    private String category;

    /**
     * Replaces the whole skills list when provided; {@code null} = unchanged.
     */
    @Size(max = 50, message = "At most 50 skills are allowed")
    private List<@NotBlank(message = "Skills must not be blank")
            @Size(max = 100, message = "Each skill must not exceed 100 characters") String> skillsRequired;

    @DecimalMin(value = "0.0", message = "Budget must be at least 0")
    @Digits(integer = 10, fraction = 2, message = "Budget must have at most 10 integer digits and 2 decimal places")
    private BigDecimal budget;

    @Size(max = 50, message = "Duration must not exceed 50 characters")
    private String duration;

    @Size(max = 50, message = "Experience level must not exceed 50 characters")
    private String experienceLevel;

    @Size(max = 100, message = "Location must not exceed 100 characters")
    private String location;

    private ProjectStatus status;

    @Future(message = "Application deadline must be in the future")
    private LocalDate applicationDeadline;
}
