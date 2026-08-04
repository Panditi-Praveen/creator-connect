package com.creatorconnect.profile.controller;

import com.creatorconnect.profile.dto.request.ProfileRequest;
import com.creatorconnect.profile.dto.request.UpdateProfileRequest;
import com.creatorconnect.profile.dto.response.ApiResponse;
import com.creatorconnect.profile.dto.response.ProfileResponse;
import com.creatorconnect.profile.security.ProfilePrincipal;
import com.creatorconnect.profile.service.ProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
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
 * REST controller exposing the Profile Service public API.
 *
 * <p>Thin by design: it delegates to {@link ProfileService}, wraps results in
 * the standard {@link ApiResponse} envelope, and derives the caller's identity
 * from the authenticated {@link ProfilePrincipal} — never from the request
 * body. Validation is triggered by {@code @Valid} and enforced by the global
 * exception handler.
 *
 * <p>Base path: {@code /profile}. All endpoints require a valid JWT issued by
 * the Auth Service.
 */
@RestController
@RequestMapping("/profile")
@Tag(name = "Profile", description = "Creator & freelancer profile management — create, view, update, delete")
@SecurityRequirement(name = "bearerAuth")
public class ProfileController {

    private final ProfileService profileService;

    /**
     * Creates the controller with its service dependency.
     *
     * @param profileService the profile business logic
     */
    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    /**
     * Creates a profile for the authenticated user.
     *
     * <p>The owning {@code userId} is taken from the JWT — a caller can never
     * create a profile on behalf of someone else. Users who already own a
     * profile receive {@code 409 CONFLICT}.
     *
     * @param request      the validated create payload
     * @param authentication the current security context
     * @param httpRequest  the raw request (used to echo the request path)
     * @return {@code 201 CREATED} with the created profile
     */
    @PostMapping
    @Operation(
            summary = "Create profile",
            description = "Creates a profile for the authenticated user. The userId is taken from the JWT, "
                    + "not from the request body. Returns 409 if the user already has a profile."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201", description = "Profile created"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400", description = "Invalid payload"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401", description = "Missing or invalid JWT"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409", description = "Profile already exists for this user")
    })
    public ResponseEntity<ApiResponse<ProfileResponse>> create(
            @Valid @RequestBody ProfileRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        ProfilePrincipal principal = (ProfilePrincipal) authentication.getPrincipal();
        ProfileResponse created = profileService.createProfile(principal.userId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        HttpStatus.CREATED.value(),
                        "Profile created successfully",
                        created,
                        httpRequest.getRequestURI()
                ));
    }

    /**
     * Returns the authenticated user's own profile.
     *
     * @param authentication the current security context
     * @param httpRequest    the raw request (used to echo the request path)
     * @return {@code 200 OK} with the caller's profile
     */
    @GetMapping("/me")
    @Operation(
            summary = "Get my profile",
            description = "Returns the profile of the authenticated user (userId from the JWT). "
                    + "Returns 404 when the user has not created a profile yet."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Profile found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401", description = "Missing or invalid JWT"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404", description = "Profile not found")
    })
    public ResponseEntity<ApiResponse<ProfileResponse>> getMyProfile(
            Authentication authentication,
            HttpServletRequest httpRequest) {

        ProfilePrincipal principal = (ProfilePrincipal) authentication.getPrincipal();
        ProfileResponse profile = profileService.getProfileByUserId(principal.userId());
        return ResponseEntity.ok(ApiResponse.success(
                HttpStatus.OK.value(),
                "Profile retrieved successfully",
                profile,
                httpRequest.getRequestURI()
        ));
    }

    /**
     * Returns the profile of any user (viewing is allowed for all
     * authenticated callers).
     *
     * @param userId      the profile owner's id
     * @param httpRequest the raw request (used to echo the request path)
     * @return {@code 200 OK} with the requested profile
     */
    @GetMapping("/{userId}")
    @Operation(
            summary = "Get profile by userId",
            description = "Returns the profile owned by the given userId. Any authenticated user may "
                    + "view any profile."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Profile found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400", description = "Invalid userId format"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401", description = "Missing or invalid JWT"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404", description = "Profile not found")
    })
    public ResponseEntity<ApiResponse<ProfileResponse>> getByUserId(
            @PathVariable UUID userId,
            HttpServletRequest httpRequest) {

        ProfileResponse profile = profileService.getProfileByUserId(userId);
        return ResponseEntity.ok(ApiResponse.success(
                HttpStatus.OK.value(),
                "Profile retrieved successfully",
                profile,
                httpRequest.getRequestURI()
        ));
    }

    /**
     * Updates the profile of the given user (owner only).
     *
     * <p>Partial-update semantics: only the non-{@code null} fields of the
     * payload are applied. Callers who are not the profile owner receive
     * {@code 403 FORBIDDEN}.
     *
     * @param userId         the profile owner's id
     * @param request        the validated partial-update payload
     * @param authentication the current security context
     * @param httpRequest    the raw request (used to echo the request path)
     * @return {@code 200 OK} with the updated profile
     */
    @PutMapping("/{userId}")
    @Operation(
            summary = "Update profile",
            description = "Partially updates the profile owned by userId. Only the authenticated owner "
                    + "may update it — anyone else receives 403. Fields omitted from the body are unchanged."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Profile updated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400", description = "Invalid payload or userId"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401", description = "Missing or invalid JWT"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403", description = "Not the profile owner"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404", description = "Profile not found")
    })
    public ResponseEntity<ApiResponse<ProfileResponse>> update(
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateProfileRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        ProfilePrincipal principal = (ProfilePrincipal) authentication.getPrincipal();
        ProfileResponse updated = profileService.updateProfile(principal.userId(), userId, request);
        return ResponseEntity.ok(ApiResponse.success(
                HttpStatus.OK.value(),
                "Profile updated successfully",
                updated,
                httpRequest.getRequestURI()
        ));
    }

    /**
     * Deletes the profile of the given user (owner only).
     *
     * @param userId         the profile owner's id
     * @param authentication the current security context
     * @param httpRequest    the raw request (used to echo the request path)
     * @return {@code 200 OK} with an empty {@code data} payload
     */
    @DeleteMapping("/{userId}")
    @Operation(
            summary = "Delete profile",
            description = "Deletes the profile owned by userId. Only the authenticated owner may delete "
                    + "it — anyone else receives 403."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Profile deleted"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400", description = "Invalid userId format"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401", description = "Missing or invalid JWT"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403", description = "Not the profile owner"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404", description = "Profile not found")
    })
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable UUID userId,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        ProfilePrincipal principal = (ProfilePrincipal) authentication.getPrincipal();
        profileService.deleteProfile(principal.userId(), userId);
        return ResponseEntity.ok(ApiResponse.success(
                HttpStatus.OK.value(),
                "Profile deleted successfully",
                null,
                httpRequest.getRequestURI()
        ));
    }
}
