package com.creatorconnect.project.controller;

import com.creatorconnect.project.dto.request.ProjectRequest;
import com.creatorconnect.project.dto.request.UpdateProjectRequest;
import com.creatorconnect.project.dto.response.ApiResponse;
import com.creatorconnect.project.dto.response.ProjectResponse;
import com.creatorconnect.project.security.ProjectPrincipal;
import com.creatorconnect.project.service.ProjectService;
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

import java.util.List;
import java.util.UUID;

/**
 * REST controller exposing the Project Service public API.
 *
 * <p>Thin by design: it delegates to {@link ProjectService}, wraps results in
 * the standard {@link ApiResponse} envelope, and derives the caller's identity
 * from the authenticated {@link ProjectPrincipal} — never from the request
 * body. Validation is triggered by {@code @Valid} and enforced by the global
 * exception handler.
 *
 * <p>Base path: {@code /projects}. All endpoints require a valid JWT issued by
 * the Auth Service.
 */
@RestController
@RequestMapping("/projects")
@Tag(name = "Project", description = "Creative project management — post, browse, update, delete")
@SecurityRequirement(name = "bearerAuth")
public class ProjectController {

    private final ProjectService projectService;

    /**
     * Creates the controller with its service dependency.
     *
     * @param projectService the project business logic
     */
    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    /**
     * Creates a project for the authenticated user.
     *
     * <p>The owning {@code userId} is taken from the JWT — a caller can never
     * post a project on behalf of someone else.
     *
     * @param request        the validated create payload
     * @param authentication the current security context
     * @param httpRequest    the raw request (used to echo the request path)
     * @return {@code 201 CREATED} with the created project
     */
    @PostMapping
    @Operation(
            summary = "Create project",
            description = "Posts a new project for the authenticated user. The userId is taken from "
                    + "the JWT, not from the request body."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201", description = "Project created"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400", description = "Invalid payload"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401", description = "Missing or invalid JWT")
    })
    public ResponseEntity<ApiResponse<ProjectResponse>> create(
            @Valid @RequestBody ProjectRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        ProjectPrincipal principal = (ProjectPrincipal) authentication.getPrincipal();
        ProjectResponse created = projectService.createProject(principal.userId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        HttpStatus.CREATED.value(),
                        "Project created successfully",
                        created,
                        httpRequest.getRequestURI()
                ));
    }

    /**
     * Returns every project, most recently created first (the browse feed).
     *
     * @param httpRequest the raw request (used to echo the request path)
     * @return {@code 200 OK} with the project list
     */
    @GetMapping
    @Operation(
            summary = "Get all projects",
            description = "Returns every project, most recently created first. Any authenticated "
                    + "user may browse the feed."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Projects retrieved"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401", description = "Missing or invalid JWT")
    })
    public ResponseEntity<ApiResponse<List<ProjectResponse>>> getAll(HttpServletRequest httpRequest) {
        List<ProjectResponse> projects = projectService.getAllProjects();
        return ResponseEntity.ok(ApiResponse.success(
                HttpStatus.OK.value(),
                "Projects retrieved successfully",
                projects,
                httpRequest.getRequestURI()
        ));
    }

    /**
     * Returns the authenticated user's own projects.
     *
     * @param authentication the current security context
     * @param httpRequest    the raw request (used to echo the request path)
     * @return {@code 200 OK} with the caller's projects
     */
    @GetMapping("/my")
    @Operation(
            summary = "Get my projects",
            description = "Returns every project owned by the authenticated user (userId from the JWT), "
                    + "most recently created first."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Projects retrieved"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401", description = "Missing or invalid JWT")
    })
    public ResponseEntity<ApiResponse<List<ProjectResponse>>> getMyProjects(
            Authentication authentication,
            HttpServletRequest httpRequest) {

        ProjectPrincipal principal = (ProjectPrincipal) authentication.getPrincipal();
        List<ProjectResponse> projects = projectService.getProjectsByUserId(principal.userId());
        return ResponseEntity.ok(ApiResponse.success(
                HttpStatus.OK.value(),
                "Projects retrieved successfully",
                projects,
                httpRequest.getRequestURI()
        ));
    }

    /**
     * Returns a single project by id (viewing is allowed for all
     * authenticated callers).
     *
     * @param projectId   the project's id
     * @param httpRequest the raw request (used to echo the request path)
     * @return {@code 200 OK} with the requested project
     */
    @GetMapping("/{id}")
    @Operation(
            summary = "Get project by id",
            description = "Returns the project with the given id. Any authenticated user may view "
                    + "any project."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Project found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400", description = "Invalid project id format"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401", description = "Missing or invalid JWT"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404", description = "Project not found")
    })
    public ResponseEntity<ApiResponse<ProjectResponse>> getById(
            @PathVariable UUID id,
            HttpServletRequest httpRequest) {

        ProjectResponse project = projectService.getProjectById(id);
        return ResponseEntity.ok(ApiResponse.success(
                HttpStatus.OK.value(),
                "Project retrieved successfully",
                project,
                httpRequest.getRequestURI()
        ));
    }

    /**
     * Updates the project with the given id (owner only).
     *
     * <p>Partial-update semantics: only the non-{@code null} fields of the
     * payload are applied. Callers who are not the project owner receive
     * {@code 403 FORBIDDEN}.
     *
     * @param id             the project's id
     * @param request        the validated partial-update payload
     * @param authentication the current security context
     * @param httpRequest    the raw request (used to echo the request path)
     * @return {@code 200 OK} with the updated project
     */
    @PutMapping("/{id}")
    @Operation(
            summary = "Update project",
            description = "Partially updates the project with the given id. Only the authenticated "
                    + "owner may update it — anyone else receives 403. Fields omitted from the body "
                    + "are unchanged."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Project updated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400", description = "Invalid payload or project id"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401", description = "Missing or invalid JWT"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403", description = "Not the project owner"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404", description = "Project not found")
    })
    public ResponseEntity<ApiResponse<ProjectResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateProjectRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        ProjectPrincipal principal = (ProjectPrincipal) authentication.getPrincipal();
        ProjectResponse updated = projectService.updateProject(principal.userId(), id, request);
        return ResponseEntity.ok(ApiResponse.success(
                HttpStatus.OK.value(),
                "Project updated successfully",
                updated,
                httpRequest.getRequestURI()
        ));
    }

    /**
     * Deletes the project with the given id (owner only).
     *
     * @param id             the project's id
     * @param authentication the current security context
     * @param httpRequest    the raw request (used to echo the request path)
     * @return {@code 200 OK} with an empty {@code data} payload
     */
    @DeleteMapping("/{id}")
    @Operation(
            summary = "Delete project",
            description = "Deletes the project with the given id. Only the authenticated owner may "
                    + "delete it — anyone else receives 403."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Project deleted"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400", description = "Invalid project id format"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401", description = "Missing or invalid JWT"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403", description = "Not the project owner"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404", description = "Project not found")
    })
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable UUID id,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        ProjectPrincipal principal = (ProjectPrincipal) authentication.getPrincipal();
        projectService.deleteProject(principal.userId(), id);
        return ResponseEntity.ok(ApiResponse.success(
                HttpStatus.OK.value(),
                "Project deleted successfully",
                null,
                httpRequest.getRequestURI()
        ));
    }
}
