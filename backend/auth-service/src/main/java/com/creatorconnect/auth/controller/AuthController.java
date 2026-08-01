package com.creatorconnect.auth.controller;

import com.creatorconnect.auth.dto.request.RegisterRequest;
import com.creatorconnect.auth.dto.response.ApiResponse;
import com.creatorconnect.auth.dto.response.RegisterResponse;
import com.creatorconnect.auth.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller exposing the Auth Service public API.
 *
 * <p>Thin by design: it delegates to {@link AuthService} and returns a
 * {@link ResponseEntity} with the proper HTTP status. Validation is triggered
 * by {@code @Valid} and enforced by the global exception handler.
 *
 * <p>Base path: {@code /auth}. This controller is intentionally kept to
 * registration for Day 4; login, me, and logout endpoints arrive in later
 * phases.
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    /**
     * Creates the controller with its service dependency.
     *
     * @param authService the registration business logic
     */
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Registers a new user.
     *
     * <p>Returns {@code 201 CREATED} with a success envelope containing the
     * persisted user projection. Duplicate emails yield {@code 409 CONFLICT},
     * invalid payloads {@code 400 BAD_REQUEST} — both handled globally.
     *
     * @param request     the validated registration payload
     * @param httpRequest the raw request (used to echo the request path)
     * @return {@code 201 CREATED} with the registration result
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<RegisterResponse>> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest httpRequest) {

        RegisterResponse registered = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        HttpStatus.CREATED.value(),
                        "User registered successfully",
                        registered,
                        httpRequest.getRequestURI()
                ));
    }
}
