/**
 * Domain exceptions and the centralized error translation.
 *
 * <p>Every custom exception is a plain {@link RuntimeException} carrying a
 * user-facing message; {@code GlobalExceptionHandler} maps each one to the
 * matching HTTP status and the standard {@code ErrorResponse} body.
 */
package com.creatorconnect.profile.exception;
