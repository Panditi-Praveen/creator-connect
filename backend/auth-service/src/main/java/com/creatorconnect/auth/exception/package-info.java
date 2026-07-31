/**
 * Domain-specific exceptions and centralized error handling.
 *
 * <p>Custom exceptions ({@code EmailAlreadyExistsException},
 * {@code UserNotFoundException}, {@code InvalidCredentialsException}) map to
 * meaningful HTTP statuses via {@code GlobalExceptionHandler}, which produces a
 * consistent JSON error body for every failure.
 */
package com.creatorconnect.auth.exception;
