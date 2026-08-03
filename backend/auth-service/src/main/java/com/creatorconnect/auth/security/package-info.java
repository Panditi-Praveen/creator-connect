/**
 * Spring Security 6 components.
 *
 * <p>Houses the JWT layer:
 * <ul>
 *   <li>{@code JwtService} — issues, parses, and validates HS256 tokens.</li>
 *   <li>{@code JwtAuthenticationFilter} — turns a validated bearer token into
 *       a {@code SecurityContext} principal.</li>
 *   <li>{@code JwtAuthenticationEntryPoint} — renders {@code 401} JSON for
 *       unauthenticated requests.</li>
 *   <li>{@code JwtAuthenticationProvider} — verifies email + password against
 *       the user table.</li>
 * </ul>
 *
 * <p>The filter, entry point, and provider are prepared but not yet wired into
 * the {@code SecurityFilterChain} (which currently lives in
 * {@code config.SecurityBeansConfig} permitting the public auth endpoints);
 * they will be enabled when protected endpoints land in Day 6.
 */
package com.creatorconnect.auth.security;
