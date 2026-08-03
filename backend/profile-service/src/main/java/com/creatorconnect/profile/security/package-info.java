/**
 * JWT-based security layer.
 *
 * <p>{@code JwtService} verifies tokens issued by the Auth Service,
 * {@code JwtAuthenticationFilter} populates the security context from verified
 * claims, {@code ProfilePrincipal} is the authenticated identity, and
 * {@code JwtAuthenticationEntryPoint} emits consistent {@code 401} bodies.
 */
package com.creatorconnect.profile.security;
