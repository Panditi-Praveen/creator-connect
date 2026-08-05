package com.creatorconnect.project.security;

import com.creatorconnect.project.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;

/**
 * JWT verification for the Project Service.
 *
 * <p>Tokens are <em>issued</em> by the Auth Service; this service only parses
 * and cryptographically verifies them with the shared HMAC key. Verified
 * tokens yield the claims the security layer needs:
 * <ul>
 *   <li>{@code sub} — the user's email address.</li>
 *   <li>{@code userId} — the owning user's UUID, the identity used to scope
 *       every project operation.</li>
 *   <li>{@code role} — CREATOR / FREELANCER / ADMIN.</li>
 * </ul>
 *
 * <p>JJWT throws {@code JwtException} on any signature/format problem, so
 * callers of {@link #isValid(String)} are expected to catch it — the prepared
 * {@link JwtAuthenticationFilter} already does.
 */
@Service
public class JwtService {

    private final JwtProperties jwtProperties;

    /**
     * Creates the service with its JWT configuration.
     *
     * @param jwtProperties the {@code app.jwt} settings (mirrors the Auth
     *                      Service's secret and issuer)
     */
    public JwtService(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    /**
     * Validates a token: signature is verified cryptographically, the issuer
     * must match the Auth Service's configured value, and the token must not
     * be expired.
     *
     * <p>Never throws — malformed, tampered, or expired tokens simply yield
     * {@code false}.
     *
     * @param token the JWT
     * @return {@code true} when the token is authentic and not expired
     */
    public boolean isValid(String token) {
        try {
            Claims claims = extractAllClaims(token);
            return !isTokenExpired(claims)
                    && Objects.equals(jwtProperties.getIssuer(), claims.getIssuer());
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }

    /**
     * Extracts the subject (user email) from a token.
     *
     * @param token the JWT
     * @return the {@code sub} claim value
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Extracts the owning user's id from a token.
     *
     * @param token the JWT
     * @return the {@code userId} claim parsed as a {@link UUID}
     * @throws IllegalArgumentException when the claim is missing or malformed
     */
    public UUID extractUserId(String token) {
        String userId = extractClaim(token, claims -> claims.get("userId", String.class));
        return UUID.fromString(userId);
    }

    /**
     * Extracts the role from a token.
     *
     * @param token the JWT
     * @return the {@code role} claim value (CREATOR/FREELANCER/ADMIN)
     */
    public String extractRole(String token) {
        return extractClaim(token, claims -> claims.get("role", String.class));
    }

    /**
     * Resolves a single claim from the token body.
     *
     * @param token    the JWT
     * @param resolver claim accessor
     * @param <T>      the resolved claim type
     * @return the resolved claim value
     */
    public <T> T extractClaim(String token, Function<Claims, T> resolver) {
        return resolver.apply(extractAllClaims(token));
    }

    /**
     * Parses and cryptographically verifies a token's claims.
     *
     * @param token the JWT
     * @return the verified claims payload
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(jwtProperties.getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Checks an already-parsed claims object against the current time.
     *
     * @param claims the verified claims
     * @return {@code true} when {@code exp} is before now
     */
    private boolean isTokenExpired(Claims claims) {
        Date expiration = claims.getExpiration();
        return expiration == null || expiration.before(new Date());
    }
}
