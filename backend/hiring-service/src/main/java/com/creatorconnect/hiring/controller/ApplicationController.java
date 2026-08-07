package com.creatorconnect.hiring.controller;

import com.creatorconnect.hiring.dto.request.ApplicationRequest;
import com.creatorconnect.hiring.dto.request.UpdateApplicationStatusRequest;
import com.creatorconnect.hiring.dto.response.ApiResponse;
import com.creatorconnect.hiring.dto.response.ApplicationResponse;
import com.creatorconnect.hiring.security.HiringPrincipal;
import com.creatorconnect.hiring.service.ApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * REST controller exposing the Hiring Service public API.
 *
 * <p>Thin by design: it delegates to {@link ApplicationService}, wraps results
 * in the standard {@link ApiResponse} envelope, and derives the caller's
 * identity from the authenticated {@link HiringPrincipal} — never from the
 * request body. Validation is triggered by {@code @Valid} and enforced by the
 * global exception handler.
 *
 * <p>Base path: {@code /applications}. All endpoints require a valid JWT
 * issued by the Auth Service.
 */
@RestController
@RequestMapping("/applications")
@Tag(name = "Application", description = "Hiring workflow — apply to projects, manage and decide on applications")
@SecurityRequirement(name = "bearerAuth")
public class ApplicationController {

    private final ApplicationService applicationService;

    /**
     * Creates the controller with its service dependency.
     *
     * @param applicationService the application business logic
     */
    public ApplicationController(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    /**
     * Applies the authenticated freelancer to a project.
     *
     * <p>The {@code freelancerId} is taken from the JWT — a caller can never
     * apply on behalf of someone else. Duplicate applications for the same
     * project yield {@code 409 CONFLICT}.
     *
     * @param request        the validated create payload
     * @param authentication the current security context
     * @param httpRequest    the raw request (used to echo the request path)
     * @return {@code 201 CREATED} with the created application
     */
    @PostMapping
    @Operation(
            summary = "Apply to project",
            description = "Submits an application for the authenticated user (freelancer only). The "
                    + "freelancerId is taken from the JWT, not from the request body. Applying twice to "
                    + "the same project yields 409."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201", description = "Application submitted"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400", description = "Invalid payload"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401", description = "Missing or invalid JWT"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403", description = "Not a freelancer"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409", description = "Already applied to this project")
    })
    public ResponseEntity<ApiResponse<ApplicationResponse>> apply(
            @Valid @RequestBody ApplicationRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        HiringPrincipal principal = (HiringPrincipal) authentication.getPrincipal();
        ApplicationResponse created = applicationService.apply(principal.userId(), principal.role(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        HttpStatus.CREATED.value(),
                        "Application submitted successfully",
                        created,
                        httpRequest.getRequestURI()
                ));
    }

    /**
     * Returns the authenticated freelancer's own applications, most recently
     * submitted first.
     *
     * <p>Supports Spring Data pagination via the {@code page} / {@code size}
     * query parameters (defaults: page 0, size 20, sorted by
     * {@code createdAt DESC, id DESC}). The page object is carried in
     * {@code data.content}.
     *
     * @param pageable     the paging/sorting specification (from query params)
     * @param authentication the current security context
     * @param httpRequest  the raw request (used to echo the request path)
     * @return {@code 200 OK} with the caller's applications
     */
    @GetMapping("/my")
    @Operation(
            summary = "Get my applications",
            description = "Returns every application submitted by the authenticated user (freelancerId "
                    + "from the JWT), most recently submitted first. Supports pagination via page/size "
                    + "query parameters (default 0/20, sorted by createdAt DESC, id DESC)."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Applications retrieved"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401", description = "Missing or invalid JWT")
    })
    public ResponseEntity<ApiResponse<Page<ApplicationResponse>>> getMyApplications(
            @PageableDefault(sort = {"createdAt", "id"}, direction = Sort.Direction.DESC, size = 20)
            Pageable pageable,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        HiringPrincipal principal = (HiringPrincipal) authentication.getPrincipal();
        Page<ApplicationResponse> applications = applicationService.getMyApplications(principal.userId(), pageable);
        return ResponseEntity.ok(ApiResponse.success(
                HttpStatus.OK.value(),
                "Applications retrieved successfully",
                applications,
                httpRequest.getRequestURI()
        ));
    }

    /**
     * Returns the applications received by a project (creator only).
     *
     * <p>Only authenticated {@code CREATOR}s may read a project's incoming
     * applications — anyone else receives {@code 403 FORBIDDEN}. Verifying the
     * creator actually owns the project is deferred to Day 6 (the owner data
     * lives in the Project Service).
     *
     * @param projectId    the project's id
     * @param pageable     the paging/sorting specification (from query params)
     * @param authentication the current security context
     * @param httpRequest  the raw request (used to echo the request path)
     * @return {@code 200 OK} with the project's applications
     */
    @GetMapping("/project/{projectId}")
    @Operation(
            summary = "Get applications for project",
            description = "Returns the applications received by the given project, most recently "
                    + "submitted first (creator only). Supports pagination via page/size query "
                    + "parameters (default 0/20)."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Applications retrieved"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400", description = "Invalid project id format"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401", description = "Missing or invalid JWT"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403", description = "Not a creator")
    })
    public ResponseEntity<ApiResponse<Page<ApplicationResponse>>> getApplicationsForProject(
            @PathVariable UUID projectId,
            @PageableDefault(sort = {"createdAt", "id"}, direction = Sort.Direction.DESC, size = 20)
            Pageable pageable,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        HiringPrincipal principal = (HiringPrincipal) authentication.getPrincipal();
        Page<ApplicationResponse> applications =
                applicationService.getApplicationsForProject(principal.role(), projectId, pageable);
        return ResponseEntity.ok(ApiResponse.success(
                HttpStatus.OK.value(),
                "Applications retrieved successfully",
                applications,
                httpRequest.getRequestURI()
        ));
    }

    /**
     * Updates the status of an application (creator decision).
     *
     * <p>Only authenticated {@code CREATOR}s may decide; the only valid
     * decisions are {@code ACCEPTED} and {@code REJECTED}, and only on
     * applications that are still {@code PENDING}.
     *
     * @param id             the application's id
     * @param request        the validated decision payload
     * @param authentication the current security context
     * @param httpRequest    the raw request (used to echo the request path)
     * @return {@code 200 OK} with the updated application
     */
    @PutMapping("/{id}/status")
    @Operation(
            summary = "Update application status",
            description = "Assigns a new status to the application with the given id (creator only). "
                    + "Valid decisions are ACCEPTED and REJECTED; the application must still be PENDING."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Application status updated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400", description = "Invalid payload, id or decision"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401", description = "Missing or invalid JWT"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403", description = "Not a creator"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404", description = "Application not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409", description = "Application is no longer pending")
    })
    public ResponseEntity<ApiResponse<ApplicationResponse>> updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateApplicationStatusRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        HiringPrincipal principal = (HiringPrincipal) authentication.getPrincipal();
        ApplicationResponse updated = applicationService.updateStatus(principal.role(), id, request);
        return ResponseEntity.ok(ApiResponse.success(
                HttpStatus.OK.value(),
                "Application status updated successfully",
                updated,
                httpRequest.getRequestURI()
        ));
    }

    /**
     * Withdraws the application with the given id (soft-delete).
     *
     * <p>Only the freelancer who submitted the application may withdraw it.
     * The record stays in the database with status {@code WITHDRAWN} so the
     * project's creator still sees it.
     *
     * @param id             the application's id
     * @param authentication the current security context
     * @param httpRequest    the raw request (used to echo the request path)
     * @return {@code 200 OK} with an empty {@code data} payload
     */
    @DeleteMapping("/{id}")
    @Operation(
            summary = "Withdraw application",
            description = "Withdraws the application with the given id (soft-delete — the status "
                    + "becomes WITHDRAWN). Only the freelancer who submitted it may withdraw it."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Application withdrawn"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400", description = "Invalid application id format"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401", description = "Missing or invalid JWT"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403", description = "Not the application's freelancer"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404", description = "Application not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409", description = "Application is no longer pending")
    })
    public ResponseEntity<ApiResponse<Void>> withdraw(
            @PathVariable UUID id,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        HiringPrincipal principal = (HiringPrincipal) authentication.getPrincipal();
        applicationService.withdraw(principal.userId(), id);
        return ResponseEntity.ok(ApiResponse.success(
                HttpStatus.OK.value(),
                "Application withdrawn successfully",
                null,
                httpRequest.getRequestURI()
        ));
    }
}
