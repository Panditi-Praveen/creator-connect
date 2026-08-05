/**
 * Exception types and the centralized error handling for the Project Service.
 *
 * <p>{@code GlobalExceptionHandler} translates every exception into the
 * standard {@code ErrorResponse} JSON contract with the matching HTTP status
 * (404, 403, 400, 409, 500, ...).
 */
package com.creatorconnect.project.exception;
