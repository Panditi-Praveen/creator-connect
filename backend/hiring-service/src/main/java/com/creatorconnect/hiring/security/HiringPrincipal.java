package com.creatorconnect.hiring.security;

import java.util.UUID;

/**
 * The authenticated identity stored in the Spring {@code SecurityContext}.
 *
 * <p>Populated by {@link JwtAuthenticationFilter} from the verified JWT claims:
 * <ul>
 *   <li>{@code userId} — the token's {@code userId} claim (UUID), the identity
 *       every application operation is scoped to.</li>
 *   <li>{@code email} — the token subject ({@code sub}).</li>
 *   <li>{@code role} — the token's {@code role} claim (CREATOR/FREELANCER/ADMIN).</li>
 * </ul>
 *
 * @param userId the owning user's id from the JWT
 * @param email  the token subject (user email)
 * @param role   the user's role from the JWT
 */
public record HiringPrincipal(UUID userId, String email, String role) {
}
