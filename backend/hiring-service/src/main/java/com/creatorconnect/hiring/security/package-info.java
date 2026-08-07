/**
 * JWT-based authentication for the Hiring Service.
 *
 * <p>Tokens are issued by the Auth Service and verified here with the shared
 * HMAC key. The filter populates the {@code SecurityContext} with a
 * {@code HiringPrincipal} (userId + email + role); the entry point writes
 * {@code 401} JSON for unauthenticated access.
 */
package com.creatorconnect.hiring.security;
