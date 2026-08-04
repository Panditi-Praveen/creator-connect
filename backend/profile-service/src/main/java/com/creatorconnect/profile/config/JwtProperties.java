package com.creatorconnect.profile.config;

import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;

/**
 * JWT configuration settings bound from the {@code app.jwt} prefix in
 * {@code application.yml}.
 *
 * <p>The Profile Service <em>validates</em> tokens issued by the Auth Service,
 * so the {@code secret} and {@code issuer} values must exactly mirror the Auth
 * Service configuration; the {@code expiration-ms} value is informational only
 * and is never used to sign tokens here.
 *
 * <p>The secret is decoded once and cached as a {@link SecretKey} so the rest
 * of the security layer never deals with raw strings.
 */
@Component
@ConfigurationProperties(prefix = "app.jwt")
@Getter
@Setter
public class JwtProperties {

    /**
     * Base64-encoded HMAC-SHA signing key (min 256 bits) shared with the Auth
     * Service that issued the tokens.
     */
    private String secret;

    /**
     * Access-token lifetime in milliseconds (informational — tokens are signed
     * by the Auth Service).
     */
    private long expirationMs;

    /**
     * Expected issuer claim written into every Auth Service token.
     */
    private String issuer;

    /**
     * Lazily decoded signing key, cached so the token path does not re-decode
     * the base64 secret on every verification operation.
     */
    private transient SecretKey cachedSigningKey;

    /**
     * Decodes the configured base64 secret into an HMAC {@link SecretKey},
     * caching the result for subsequent calls.
     *
     * @return the signing key used to verify incoming tokens
     */
    public SecretKey getSigningKey() {
        if (cachedSigningKey == null) {
            cachedSigningKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        }
        return cachedSigningKey;
    }
}
