package com.creatorconnect.auth.security;

import com.creatorconnect.auth.config.JwtProperties;
import com.creatorconnect.auth.entity.Role;
import com.creatorconnect.auth.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link JwtService}.
 *
 * <p>Exercises token generation, signature/expiry validation, claim extraction,
 * and the configured lifetime — no Spring context or database involved.
 */
class JwtServiceTest {

    private static final String SECRET =
            "Y3JlYXRvcmNvbm5lY3Qtc3VwZXItc2VjcmV0LWtleS1jaGFuZ2UtbWUtYmVmb3JlLXByb2R1Y3Rpb24tMjAyNg==";
    private static final String ISSUER = "creatorconnect-auth-service";

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
    void generateToken_producesValidTokenWithExpectedClaims() {
        User user = User.builder()
                .id(UUID.fromString("7b092f57-a53d-46dd-b2e0-4c8f0289fb91"))
                .email("praveen@gmail.com")
                .role(Role.CREATOR)
                .enabled(true)
                .build();

        String token = jwtService.generateToken(user);

        String role = jwtService.extractClaim(token, claims -> claims.get("role", String.class));
        String userId = jwtService.extractClaim(token, claims -> claims.get("userId", String.class));

        assertThat(jwtService.isValid(token)).isTrue();
        assertThat(jwtService.extractUsername(token)).isEqualTo("praveen@gmail.com");
        assertThat(role).isEqualTo("CREATOR");
        assertThat(userId).isEqualTo("7b092f57-a53d-46dd-b2e0-4c8f0289fb91");
    }

    @Test
    void extractExpiration_returnsTheConfiguredLifetime() {
        User user = User.builder()
                .id(UUID.randomUUID())
                .email("creator@example.com")
                .role(Role.CREATOR)
                .enabled(true)
                .build();

        String token = jwtService.generateToken(user);
        long millis = jwtService.extractExpiration(token).getTime()
                - jwtService.extractClaim(token, claims -> claims.getIssuedAt()).getTime();

        assertThat(millis).isEqualTo(86_400_000L);
    }

    @Test
    void isValid_rejectsTokenSignedWithDifferentKey() {
        JwtProperties other = new JwtProperties();
        other.setSecret("c2hvcnQtb3RoZXItc2VjcmV0LXRvby1zaG9ydC1mb3ItaHMtMjU2LWtleS1hYQ==");
        other.setExpirationMs(86_400_000L);
        other.setIssuer(ISSUER);
        JwtService otherService = new JwtService(other);

        User user = User.builder()
                .id(UUID.randomUUID())
                .email("creator@example.com")
                .role(Role.CREATOR)
                .enabled(true)
                .build();
        String foreignToken = otherService.generateToken(user);

        assertThat(jwtService.isValid(foreignToken)).isFalse();
    }

    @Test
    void isTokenExpired_returnsFalseForFreshToken() {
        User user = User.builder()
                .id(UUID.randomUUID())
                .email("creator@example.com")
                .role(Role.CREATOR)
                .enabled(true)
                .build();

        String token = jwtService.generateToken(user);

        assertThat(jwtService.isTokenExpired(token)).isFalse();
        assertThat(jwtService.isValid(token)).isTrue();
    }

    @Test
    void isTokenExpired_returnsTrueForPastExpiration() {
        jwtProperties.setExpirationMs(-1L);
        jwtService = new JwtService(jwtProperties);

        User user = User.builder()
                .id(UUID.randomUUID())
                .email("creator@example.com")
                .role(Role.CREATOR)
                .enabled(true)
                .build();
        String token = jwtService.generateToken(user);

        assertThat(jwtService.isTokenExpired(token)).isTrue();
        assertThat(jwtService.isValid(token)).isFalse();
    }

    @Test
    void getExpirationSeconds_returnsLifetimeInSeconds() {
        assertThat(jwtService.getExpirationSeconds()).isEqualTo(86_400L);
    }
}
