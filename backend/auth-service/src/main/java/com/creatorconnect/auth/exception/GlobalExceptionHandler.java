package com.creatorconnect.auth.exception;

import com.creatorconnect.auth.dto.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.stream.Collectors;

/**
 * Centralized error handling for the whole Auth Service.
 *
 * <p>Every exception is translated into a consistent {@link ErrorResponse}
 * body {@code { timestamp, status, error, message, path }} with the matching
 * HTTP status:
 * <ul>
 *   <li>{@link MethodArgumentNotValidException} &rarr; {@code 400 BAD_REQUEST}
 *       with the collected field-level validation errors.</li>
 *   <li>{@link HttpMessageNotReadableException} &rarr; {@code 400 BAD_REQUEST}
 *       for malformed JSON (e.g. an unknown role value).</li>
 *   <li>{@link EmailAlreadyExistsException} &rarr; {@code 409 CONFLICT}.</li>
 *   <li>{@link UserNotFoundException} &amp; {@link InvalidCredentialsException}
 *       &rarr; {@code 401 UNAUTHORIZED} (login failures).</li>
 *   <li>Anything else &rarr; {@code 500 INTERNAL_SERVER_ERROR} with a generic
 *       message (the real cause is logged server-side).</li>
 * </ul>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handles a duplicate-email registration attempt.
     *
     * @param ex      the thrown exception
     * @param request the originating HTTP request
     * @return {@code 409 CONFLICT} with the exception message
     */
    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleEmailAlreadyExists(EmailAlreadyExistsException ex,
                                                                  HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), request);
    }

    /**
     * Handles login attempts with an unknown email address.
     *
     * <p>Returns the same status as {@link #handleInvalidCredentials} so the
     * response does not reveal whether an email is registered.
     *
     * @param ex      the thrown exception
     * @param request the originating HTTP request
     * @return {@code 401 UNAUTHORIZED} with the exception message
     */
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFound(UserNotFoundException ex,
                                                            HttpServletRequest request) {
        return build(HttpStatus.UNAUTHORIZED, ex.getMessage(), request);
    }

    /**
     * Handles login attempts with a wrong password or a disabled account.
     *
     * @param ex      the thrown exception
     * @param request the originating HTTP request
     * @return {@code 401 UNAUTHORIZED} with the exception message
     */
    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCredentials(InvalidCredentialsException ex,
                                                                  HttpServletRequest request) {
        return build(HttpStatus.UNAUTHORIZED, ex.getMessage(), request);
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
     * Handles constraint violations raised at flush time (e.g. the unique index
     * on {@code users.email}).
     *
     * <p>Covers the race between the {@code existsByEmail} check and the insert:
     * if two identical registrations arrive concurrently, the database constraint
     * wins and must still surface as {@code 409 CONFLICT} rather than 500.
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
     * <p>Covers {@code NoResourceFoundException} (unknown paths &rarr; 404) and
     * any future {@code ResponseStatusException} (e.g. 401/403 from the JWT
     * layer in later phases). Without this, those would fall into the generic
     * handler and incorrectly surface as 500.
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
