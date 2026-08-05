package com.creatorconnect.project.security;

import com.creatorconnect.project.config.JwtProperties;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the Project Service {@link JwtService}.
 *
 * <p>Exercises verification of tokens signed with the shared HMAC key — the
 * same secret used by the Auth Service to issue tokens. Tokens are built
 * inline with JJWT (the service under test only parses/verifies). No Spring
 * context or database involved.
 */
class JwtServiceTest {

    private static final String SECRET =
            "Y3JlYXRvcmNvbm5lY3Qtc3VwZXItc2VjcmV0LWtleS1jaGFuZ2UtbWUtYmVmb3JlLXByb2R1Y3Rpb24tMjAyNg==";
    private static final String ISSUER = "creatorconnect-auth-service";
    private static final UUID USER_ID = UUID.fromString("7b092f57-a53d-46dd-b2e0-4c8f0289fb91");
    private static final String EMAIL = "praveen@gmail.com";
    private static final String ROLE = "CREATOR";

    private JwtProperties jwtProperties;
    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtProperties = new JwtProperties();
        jwtProperties.setSecret(SECRET);
        jwtProperties.setExpirationMs(86_400_000L);
        jwtProperties.setIssuer(ISSUER);
        jwtService = new JwtService(jwtProperties);
    }

    @Test
    void isValid_acceptsTokenSignedWithSharedKey() {
        String token = buildToken(EMAIL, claims(), futureExpiration());

        assertThat(jwtService.isValid(token)).isTrue();
    }

    @Test
    void extractsClaimsFromValidToken() {
        String token = buildToken(EMAIL, claims(), futureExpiration());

        assertThat(jwtService.extractUsername(token)).isEqualTo(EMAIL);
        assertThat(jwtService.extractUserId(token)).isEqualTo(USER_ID);
        assertThat(jwtService.extractRole(token)).isEqualTo(ROLE);
    }

    @Test
    void isValid_rejectsTokenSignedWithDifferentKey() {
        SecretKey otherKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(
                "c2hvcnQtb3RoZXItc2VjcmV0LXRvby1zaG9ydC1mb3ItaHMtMjU2LWtleS1hYQ=="));
        String foreignToken = Jwts.builder()
                .claims(claims())
                .subject(EMAIL)
                .issuer(ISSUER)
                .issuedAt(new Date())
                .expiration(futureExpiration())
                .signWith(otherKey, Jwts.SIG.HS256)
                .compact();

        assertThat(jwtService.isValid(foreignToken)).isFalse();
    }

    @Test
    void isValid_rejectsTokenWithWrongIssuer() {
        String token = Jwts.builder()
                .claims(claims())
                .subject(EMAIL)
                .issuer("some-other-issuer")
                .issuedAt(new Date())
                .expiration(futureExpiration())
                .signWith(jwtProperties.getSigningKey(), Jwts.SIG.HS256)
                .compact();

        assertThat(jwtService.isValid(token)).isFalse();
    }

    @Test
    void isValid_rejectsExpiredToken() {
        String token = buildToken(EMAIL, claims(), new Date(System.currentTimeMillis() - 60_000L));

        assertThat(jwtService.isValid(token)).isFalse();
    }

    @Test
    void isValid_rejectsGarbageToken() {
        assertThat(jwtService.isValid("not-a-jwt")).isFalse();
    }

    private Map<String, Object> claims() {
        return Map.of(
                "userId", USER_ID.toString(),
                "role", ROLE
        );
    }

    private String buildToken(String subject, Map<String, Object> claims, Date expiration) {
        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuer(ISSUER)
                .issuedAt(new Date())
                .expiration(expiration)
                .signWith(jwtProperties.getSigningKey(), Jwts.SIG.HS256)
                .compact();
    }

    private Date futureExpiration() {
        return new Date(System.currentTimeMillis() + 86_400_000L);
    }
}
