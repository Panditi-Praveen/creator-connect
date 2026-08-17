package com.creatorconnect.ai.exception;

import com.creatorconnect.ai.dto.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

/**
 * Centralized error handling for the whole AI Service.
 *
 * <p>Every exception is translated into a consistent {@link ErrorResponse}
 * body {@code { timestamp, status, error, message, path }} with the matching
 * HTTP status:
 * <ul>
 *   <li>{@link AiConfigurationException} &rarr; {@code 503} — LLM not
 *       configured (client-safe message, no secrets).</li>
 *   <li>{@link AiLlmException} &rarr; {@code 502} — LLM call failed or the
 *       response could not be parsed.</li>
 *   <li>{@link ProfileServiceUnavailableException} &rarr; {@code 503} —
 *       Profile Service unreachable.</li>
 *   <li>{@link MethodArgumentNotValidException} &rarr; {@code 400} with the
 *       collected field-level validation errors.</li>
 *   <li>{@link HttpMessageNotReadableException} &amp; type mismatches &rarr;
 *       {@code 400} for malformed bodies / bad parameters.</li>
 *   <li>Unknown or trailing-slash paths &rarr; {@code 404} via
 *       {@code NoResourceFoundException}.</li>
 *   <li>Anything else &rarr; {@code 500} with a generic message (the real
 *       cause is logged server-side).</li>
 * </ul>
 *
 * <p>Stack traces and secrets are never exposed to clients.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handles missing AI configuration (e.g. no {@code OPENAI_API_KEY}).
     *
     * @param ex      the thrown exception
     * @param request the originating HTTP request
     * @return {@code 503 SERVICE_UNAVAILABLE} with the exception message
     */
    @ExceptionHandler(AiConfigurationException.class)
    public ResponseEntity<ErrorResponse> handleAiConfiguration(AiConfigurationException ex,
                                                               HttpServletRequest request) {
        return build(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage(), request);
    }

    /**
     * Handles external LLM failures (network, provider error, unparseable
     * response).
     *
     * @param ex      the thrown exception
     * @param request the originating HTTP request
     * @return {@code 502 BAD_GATEWAY} with a generic message
     */
    @ExceptionHandler(AiLlmException.class)
    public ResponseEntity<ErrorResponse> handleAiLlm(AiLlmException ex,
                                                     HttpServletRequest request) {
        log.error("LLM call failed while processing request {}", request.getRequestURI(), ex);
        return build(HttpStatus.BAD_GATEWAY, "AI service is temporarily unavailable", request);
    }

    /**
     * Handles an unreachable Profile Service.
     *
     * @param ex      the thrown exception
     * @param request the originating HTTP request
     * @return {@code 503 SERVICE_UNAVAILABLE} with a generic message
     */
    @ExceptionHandler(ProfileServiceUnavailableException.class)
    public ResponseEntity<ErrorResponse> handleProfileServiceUnavailable(ProfileServiceUnavailableException ex,
                                                                         HttpServletRequest request) {
        return build(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage(), request);
    }

    /**
     * Handles payloads that fail Jakarta Bean Validation.
     *
     * @param ex      the validation exception
     * @param request the originating HTTP request
     * @return {@code 400 BAD_REQUEST} listing every failing field
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex,
                                                          HttpServletRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + " " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return build(HttpStatus.BAD_REQUEST, message, request);
    }

    /**
     * Handles malformed or unreadable JSON request bodies.
     *
     * @param request the originating HTTP request
     * @return {@code 400 BAD_REQUEST} with a generic malformed-body message
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadableBody(HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "Malformed request body", request);
    }

    /**
     * Handles request parameters that cannot be converted to the target type.
     *
     * @param ex      the type conversion exception
     * @param request the originating HTTP request
     * @return {@code 400 BAD_REQUEST} naming the offending variable
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex,
                                                            HttpServletRequest request) {
        String name = ex.getName() == null ? "argument" : ex.getName();
        return build(HttpStatus.BAD_REQUEST, "Invalid value for parameter '" + name + "'", request);
    }

    /**
     * Handles framework-level type mismatches.
     *
     * @param ex      the thrown exception
     * @param request the originating HTTP request
     * @return {@code 400 BAD_REQUEST} with a generic message
     */
    @ExceptionHandler(TypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(TypeMismatchException ex,
                                                            HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "Invalid request parameter value", request);
    }

    /**
     * Handles requests that use an HTTP method the route does not support.
     *
     * @param ex      the thrown exception
     * @param request the originating HTTP request
     * @return {@code 405 METHOD_NOT_ALLOWED} with the exception message
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex,
                                                                  HttpServletRequest request) {
        return build(HttpStatus.METHOD_NOT_ALLOWED, ex.getMessage(), request);
    }

    /**
     * Handles entity-level Bean Validation failures raised at flush time
     * (defense-in-depth).
     *
     * @param ex      the thrown exception
     * @param request the originating HTTP request
     * @return {@code 400 BAD_REQUEST} listing every failing constraint
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex,
                                                                   HttpServletRequest request) {
        String message = ex.getConstraintViolations().stream()
                .map(violation -> violation.getPropertyPath() + " " + violation.getMessage())
                .collect(Collectors.joining("; "));
        return build(HttpStatus.BAD_REQUEST, message, request);
    }

    /**
     * Handles requests that match no controller route and no static resource
     * (e.g. a trailing-slash base path such as {@code /ai/}). Spring 6 no
     * longer matches trailing slashes against controller mappings, so such
     * paths fall through to the resource handler, which throws
     * {@link NoResourceFoundException}.
     *
     * @param ex      the thrown exception
     * @param request the originating HTTP request
     * @return {@code 404 NOT_FOUND} with a generic resource message
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResourceFound(NoResourceFoundException ex,
                                                               HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, "Resource not found", request);
    }

    /**
     * Preserves the status of framework exceptions that already carry one.
     *
     * @param ex      the thrown exception
     * @param request the originating HTTP request
     * @return the exception's own status with its reason as the message
     */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatus(ResponseStatusException ex, HttpServletRequest request) {
        HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
        if (status == null) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }
        String message = (ex.getReason() == null || ex.getReason().isBlank())
                ? status.getReasonPhrase()
                : ex.getReason();
        return build(status, message, request);
    }

    /**
     * Catch-all for any unexpected exception.
     *
     * @param ex      the thrown exception
     * @param request the originating HTTP request
     * @return {@code 500 INTERNAL_SERVER_ERROR} with a generic message
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception while processing request {}", request.getRequestURI(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", request);
    }

    /**
     * Assembles a {@link ResponseEntity} from the standard error contract.
     *
     * @param status  the HTTP status
     * @param message the error message
     * @param request the originating HTTP request (for the {@code path})
     * @return the response entity
     */
    private ResponseEntity<ErrorResponse> build(HttpStatus status, String message, HttpServletRequest request) {
        return ResponseEntity.status(status)
                .body(ErrorResponse.of(status, message, request.getRequestURI()));
    }
}
