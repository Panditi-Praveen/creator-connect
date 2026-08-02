package com.creatorconnect.auth.security;

import com.creatorconnect.auth.config.JwtProperties;
import com.creatorconnect.auth.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Map;
import java.util.function.Function;

/**
 * Central JWT operations for the Auth Service.
 *
 * <p>Wraps the JJWT 0.12.x fluent API to issue and verify HS256-signed access
 * tokens. All tokens are signed with the configured HMAC key and carry:
 * <ul>
 *   <li>{@code sub} — the user's (lowercase) email address.</li>
 *   <li>{@code userId} / {@code role} — claims used by role-based access
 *       control in later phases.</li>
 *   <li>{@code iss} — the configured issuer.</li>
 *   <li>{@code iat} / {@code exp} — issue and expiry timestamps.</li>
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
     * @param jwtProperties the {@code app.jwt} settings
     */
    public JwtService(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    /**
     * Issues an access token for the given user.
     *
     * @param user the authenticated user
     * @return a signed HS256 JWT valid for the configured lifetime
     */
    public String generateToken(User user) {
        return generateToken(
                user.getEmail(),
                Map.of(
                        "userId", user.getId().toString(),
                        "role", user.getRole().name()
                )
        );
    }

    /**
     * Issues an access token with explicit claims.
     *
     * @param subject the token subject (the user email)
     * @param claims  additional claims embedded in the token body
     * @return a signed HS256 JWT valid for the configured lifetime
     */
    public String generateToken(String subject, Map<String, Object> claims) {
        Date now = new Date();
        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuer(jwtProperties.getIssuer())
                .issuedAt(now)
                .expiration(new Date(now.getTime() + jwtProperties.getExpirationMs()))
                .signWith(jwtProperties.getSigningKey(), Jwts.SIG.HS256)
                .compact();
    }

    /**
     * Returns the configured token lifetime in seconds.
     *
     * @return the access-token lifetime, used by clients to schedule refreshes
     */
    public long getExpirationSeconds() {
        return jwtProperties.getExpirationMs() / 1000;
    }

    /**
     * Extracts the subject (username/email) from a token.
     *
     * @param token the JWT
     * @return the {@code sub} claim value
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Extracts the expiry timestamp from a token.
     *
     * @param token the JWT
     * @return the {@code exp} claim value
     */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Validates a token: signature is verified cryptographically, the issuer
     * must match the configured value, and the token must not be expired.
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
                    && jwtProperties.getIssuer().equals(claims.getIssuer());
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }

    /**
     * Checks whether a token has passed its expiry time.
     *
     * <p>JJWT reports an expired token by throwing
     * {@link ExpiredJwtException} during parsing, which is translated into
     * {@code true} here. Other parsing failures propagate to the caller.
     *
     * @param token the JWT
     * @return {@code true} when the token is expired
     */
    public boolean isTokenExpired(String token) {
        try {
            return isTokenExpired(extractAllClaims(token));
        } catch (ExpiredJwtException ex) {
            return true;
        }
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
        return claims.getExpiration().before(new Date());
    }
}
