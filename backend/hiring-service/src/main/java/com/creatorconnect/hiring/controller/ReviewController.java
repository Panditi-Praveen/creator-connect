package com.creatorconnect.hiring.controller;

import com.creatorconnect.hiring.dto.request.ReviewRequest;
import com.creatorconnect.hiring.dto.response.ApiResponse;
import com.creatorconnect.hiring.dto.response.FreelancerReviewsResponse;
import com.creatorconnect.hiring.dto.response.ReviewResponse;
import com.creatorconnect.hiring.security.HiringPrincipal;
import com.creatorconnect.hiring.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * REST controller exposing the Hiring Service review API.
 *
 * <p>Thin by design: it delegates to {@link ReviewService}, wraps results in
 * the standard {@link ApiResponse} envelope, and derives the caller's
 * identity from the authenticated {@link HiringPrincipal} — never from the
 * request body. Validation is triggered by {@code @Valid} and enforced by the
 * global exception handler.
 *
 * <p>Base path: {@code /reviews}. All endpoints require a valid JWT issued by
 * the Auth Service.
 */
@RestController
@RequestMapping("/reviews")
@Tag(name = "Review", description = "Reviews & ratings — creators review freelancers they hired, anyone reads them")
@SecurityRequirement(name = "bearerAuth")
public class ReviewController {

    private final ReviewService reviewService;

    /**
     * Creates the controller with its service dependency.
     *
     * @param reviewService the review business logic
     */
    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    /**
     * Submits a review on behalf of the authenticated creator.
     *
     * <p>The {@code creatorId} is taken from the JWT — a caller can never
     * review on behalf of someone else. Only the creator who owns the project
     * may review on it (ownership is verified against the Project Service).
     * The freelancer must hold an {@code ACCEPTED} application on the project
     * (be hired) and may only be reviewed once per project; violations yield
     * {@code 403}, {@code 400} and {@code 409} respectively.
     *
     * @param request        the validated create payload
     * @param authentication the current security context
     * @param httpRequest    the raw request (used to echo the request path)
     * @return {@code 201 CREATED} with the created review
     */
    @PostMapping
    @Operation(
            summary = "Submit review",
            description = "Submits a review for the authenticated user. Only the creator who owns the "
                    + "project (verified against the Project Service) may review on it; the "
                    + "creatorId is taken from the JWT, not from the request body. The freelancer "
                    + "must have an ACCEPTED application on the project (be hired), and may only be "
                    + "reviewed once per project."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201", description = "Review submitted"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400", description = "Invalid payload or freelancer not hired on this project"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401", description = "Missing or invalid JWT"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403", description = "Not a creator or not the project owner"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404", description = "Project not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409", description = "Freelancer already reviewed on this project"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "503", description = "Project Service unavailable")
    })
    public ResponseEntity<ApiResponse<ReviewResponse>> submitReview(
            @Valid @RequestBody ReviewRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        HiringPrincipal principal = (HiringPrincipal) authentication.getPrincipal();
        ReviewResponse created = reviewService.createReview(principal.userId(), principal.role(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        HttpStatus.CREATED.value(),
                        "Review submitted successfully",
                        created,
                        httpRequest.getRequestURI()
                ));
    }

    /**
     * Returns every review received by the freelancer (newest first) together
     * with the average rating across them.
     *
     * <p>Any authenticated user may read the summary. When the freelancer has
     * no reviews yet, {@code reviews} is empty, {@code totalReviews} is
     * {@code 0} and {@code averageRating} is {@code 0.0}.
     *
     * @param freelancerId the reviewed freelancer's id
     * @param httpRequest  the raw request (used to echo the request path)
     * @return {@code 200 OK} with the aggregated review summary
     */
    @GetMapping("/freelancer/{freelancerId}")
    @Operation(
            summary = "Get freelancer reviews",
            description = "Returns every review received by the freelancer (newest first) together with "
                    + "the average rating across them. Any authenticated user may read the summary."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Reviews retrieved"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400", description = "Invalid freelancer id format"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401", description = "Missing or invalid JWT")
    })
    public ResponseEntity<ApiResponse<FreelancerReviewsResponse>> getFreelancerReviews(
            @PathVariable UUID freelancerId,
            HttpServletRequest httpRequest) {

        FreelancerReviewsResponse reviews = reviewService.getFreelancerReviews(freelancerId);
        return ResponseEntity.ok(ApiResponse.success(
                HttpStatus.OK.value(),
                "Reviews retrieved successfully",
                reviews,
                httpRequest.getRequestURI()
        ));
    }
}
