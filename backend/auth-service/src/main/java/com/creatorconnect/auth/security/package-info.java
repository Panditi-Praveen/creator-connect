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
 * <p>The filter and entry point are wired into the {@code SecurityFilterChain}
 * in {@code config.SecurityBeansConfig}: the filter runs before
 * {@code UsernamePasswordAuthenticationFilter} and the entry point answers
 * unauthenticated requests with {@code 401} JSON. The provider is registered
 * with the global {@code AuthenticationManager} exposed by the same config.
 * Public endpoints ({@code /auth/register}, {@code /auth/login}, actuator,
 * Swagger/OpenAPI) are permitted; every other route requires a valid bearer
 * token.
 */
package com.creatorconnect.auth.security;
