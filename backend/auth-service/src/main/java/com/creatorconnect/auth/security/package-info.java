/**
 * Spring Security 6 components.
 *
 * <p>Houses the {@code JwtAuthenticationFilter}, {@code UserDetailsService}
 * implementation, and security utilities that extract the authenticated user
 * from a validated JWT. The {@code SecurityFilterChain} bean currently lives in
 * {@code config.SecurityBeansConfig} (Day 4 baseline permitting public
 * endpoints) and will be tightened to bearer-token auth when JWT lands.
 */
package com.creatorconnect.auth.security;
