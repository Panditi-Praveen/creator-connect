package com.creatorconnect.hiring.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Request payload for {@code POST /applications}.
 *
 * <p>Carries everything a freelancer submits when applying to a project:
 * {@code projectId}, {@code proposal}, {@code expectedBudget} and
 * {@code estimatedDuration} are mandatory.
 *
 * <p>The {@code freelancerId} is deliberately <em>not</em> part of this DTO —
 * it is taken from the authenticated JWT so a caller can never apply on
 * someone else's behalf. The application's {@code status} is not settable at
 * creation either; it always starts as {@code PENDING}.
 *
 * <p>Validation failures are translated into {@code 400 BAD_REQUEST} responses
 * by {@code GlobalExceptionHandler}.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationRequest {

    @NotNull(message = "Project id is required")
    private UUID projectId;

    @NotBlank(message = "Proposal is required")
    @Size(max = 5000, message = "Proposal must not exceed 5000 characters")
    private String proposal;

    @NotNull(message = "Expected budget is required")
    @DecimalMin(value = "0.0", message = "Expected budget must be at least 0")
    @Digits(integer = 10, fraction = 2, message = "Expected budget must have at most 10 integer digits and 2 decimal places")
    private BigDecimal expectedBudget;

    @NotBlank(message = "Estimated duration is required")
    @Size(max = 50, message = "Estimated duration must not exceed 50 characters")
    private String estimatedDuration;
}
