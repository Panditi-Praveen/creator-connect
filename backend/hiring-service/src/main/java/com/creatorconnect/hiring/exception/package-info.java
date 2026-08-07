/**
 * Domain exceptions and the centralized error handling for the Hiring Service.
 *
 * <p>Every exception is translated into a consistent {@code ErrorResponse}
 * body by {@code GlobalExceptionHandler}: not found &rarr; 404, access denied
 * &rarr; 403, duplicates and illegal status transitions &rarr; 409, and
 * business-rule validation failures &rarr; 400.
 */
package com.creatorconnect.hiring.exception;
