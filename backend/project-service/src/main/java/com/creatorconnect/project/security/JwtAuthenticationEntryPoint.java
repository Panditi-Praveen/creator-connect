package com.creatorconnect.project.security;

import com.creatorconnect.project.dto.response.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Security entry point that turns unauthenticated access into a clean
 * {@code 401 UNAUTHORIZED} JSON response.
 *
 * <p>Spring Security invokes this class whenever an unauthenticated request
 * reaches a protected route (e.g. a missing, malformed, or expired JWT).
 *
 * <p>The body uses the same {@link ErrorResponse} contract as the global
 * exception handler so clients see one consistent error shape.
 */
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    /**
     * Creates the entry point with a JSON serializer.
     *
     * @param objectMapper the Spring-managed Jackson mapper
     */
    public JwtAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        ErrorResponse error = ErrorResponse.of(
                HttpStatus.UNAUTHORIZED,
                "Authentication required",
                request.getRequestURI()
        );
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), error);
    }
}
