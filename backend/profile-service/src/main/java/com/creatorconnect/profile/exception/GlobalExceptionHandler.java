package com.creatorconnect.profile.exception;

import com.creatorconnect.profile.dto.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

import java.util.stream.Collectors;

/**
 * Centralized error handling for the whole Profile Service.
 *
 * <p>Every exception is translated into a consistent {@link ErrorResponse}
 * body {@code { timestamp, status, error, message, path }} with the matching
 * HTTP status:
 * <ul>
 *   <li>{@link ProfileNotFoundException} &rarr; {@code 404 NOT_FOUND}.</li>
 *   <li>{@link ProfileAccessDeniedException} &rarr; {@code 403 FORBIDDEN}.</li>
 *   <li>{@link ProfileAlreadyExistsException} &rarr; {@code 409 CONFLICT}.</li>
 *   <li>{@link MethodArgumentNotValidException} &rarr; {@code 400 BAD_REQUEST}
 *       with the collected field-level validation errors.</li>
 *   <li>{@link HttpMessageNotReadableException} &amp; type mismatches &rarr;
 *       {@code 400 BAD_REQUEST} for malformed bodies / bad path variables.</li>
 *   <li>Anything else &rarr; {@code 500 INTERNAL_SERVER_ERROR} with a generic
 *       message (the real cause is logged server-side).</li>
 * </ul>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handles requests for a profile that does not exist.
     *
     * @param ex      the thrown exception
     * @param request the originating HTTP request
     * @return {@code 404 NOT_FOUND} with the exception message
     */
    @ExceptionHandler(ProfileNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleProfileNotFound(ProfileNotFoundException ex,
                                                               HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    /**
     * Handles attempts to modify or delete a profile owned by another user.
     *
     * @param ex      the thrown exception
     * @param request the originating HTTP request
     * @return {@code 403 FORBIDDEN} with the exception message
     */
    @ExceptionHandler(ProfileAccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleProfileAccessDenied(ProfileAccessDeniedException ex,
                                                                   HttpServletRequest request) {
        return build(HttpStatus.FORBIDDEN, ex.getMessage(), request);
    }

    /**
     * Handles attempts to create a second profile for the same user.
     *
     * @param ex      the thrown exception
     * @param request the originating HTTP request
     * @return {@code 409 CONFLICT} with the exception message
     */
    @ExceptionHandler(ProfileAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleProfileAlreadyExists(ProfileAlreadyExistsException ex,
                                                                    HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), request);
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
     * Handles path variables that cannot be converted to the target type
     * (e.g. a non-UUID {@code userId} in {@code /profile/{userId}}).
     *
     * @param ex      the type conversion exception
     * @param request the originating HTTP request
     * @return {@code 400 BAD_REQUEST} naming the offending variable
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex,
                                                            HttpServletRequest request) {
        String name = ex.getName() == null ? "argument" : ex.getName();
        return build(HttpStatus.BAD_REQUEST, "Invalid value for path variable '" + name + "'", request);
    }

    /**
     * Handles framework-level type mismatches (covers the same ground as
     * {@link #handleTypeMismatch} for non-path arguments).
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
     * (defense-in-depth — request DTOs are validated first, so this only
     * triggers when a caller bypasses the DTO constraints).
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
     * Handles constraint violations raised at flush time (e.g. the unique
     * index on {@code profiles.user_id}).
     *
     * <p>Covers the race between the {@code existsByUserId} check and the
     * insert: if two identical creates arrive concurrently, the database
     * constraint wins and must still surface as {@code 409 CONFLICT} rather
     * than 500.
     *
     * @param request the originating HTTP request
     * @return {@code 409 CONFLICT} with a duplicate-resource message
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, "Resource already exists or violates a data constraint", request);
    }

    /**
     * Preserves the status of framework exceptions that already carry one.
     *
     * <p>Covers {@code NoResourceFoundException} (unknown paths &rarr; 404)
     * and any future {@code ResponseStatusException}.
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
