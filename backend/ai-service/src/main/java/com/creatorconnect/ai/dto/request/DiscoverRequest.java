package com.creatorconnect.ai.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request payload for {@code POST /ai/discover}.
 *
 * <p>Carries the natural-language hiring/talent query. The only field is the
 * query itself; validation failures are translated into {@code 400
 * BAD_REQUEST} responses by {@code GlobalExceptionHandler}.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiscoverRequest {

    @NotBlank(message = "Query is required")
    @Size(max = 500, message = "Query must not exceed 500 characters")
    private String query;
}
