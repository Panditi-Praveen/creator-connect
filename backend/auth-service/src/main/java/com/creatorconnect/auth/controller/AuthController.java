package com.creatorconnect.auth.controller;

import com.creatorconnect.auth.dto.request.LoginRequest;
import com.creatorconnect.auth.dto.request.RegisterRequest;
import com.creatorconnect.auth.dto.response.ApiResponse;
import com.creatorconnect.auth.dto.response.LoginResponse;
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
 * <p>Base path: {@code /auth}. Day 4 delivered registration; Day 5 adds the
 * login endpoint that issues JWTs. Me/logout endpoints arrive in later
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

    /**
     * Authenticates a user and issues a JWT access token.
     *
     * <p>Returns {@code 200 OK} with a success envelope carrying the token
     * response. Unknown emails, wrong passwords, and disabled accounts yield
     * {@code 401 UNAUTHORIZED}; malformed payloads {@code 400 BAD_REQUEST} —
     * both handled globally.
     *
     * @param request     the validated login payload
     * @param httpRequest the raw request (used to echo the request path)
     * @return {@code 200 OK} with the issued token and user projection
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {

        LoginResponse loggedIn = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success(
                HttpStatus.OK.value(),
                "Login successful",
                loggedIn,
                httpRequest.getRequestURI()
        ));
    }
}
