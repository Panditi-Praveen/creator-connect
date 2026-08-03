package com.creatorconnect.auth.config;

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
 * <p>Exposes the raw settings consumed by {@code security.JwtService}:
 * <ul>
 *   <li>{@code secret} — a base64-encoded HMAC key (HS256 requires at least
 *       256 bits / 32 bytes).</li>
 *   <li>{@code expiration-ms} — access-token lifetime in milliseconds.</li>
 *   <li>{@code issuer} — the token issuer claim ({@code iss}).</li>
 * </ul>
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
     * Base64-encoded HMAC-SHA signing key (min 256 bits).
     */
    private String secret;

    /**
     * Access-token lifetime in milliseconds.
     */
    private long expirationMs;

    /**
     * Issuer claim written into every issued token.
     */
    private String issuer;

    /**
     * Lazily decoded signing key, cached so the token path does not re-decode
     * the base64 secret on every sign/verify operation.
     */
    private transient SecretKey cachedSigningKey;

    /**
     * Decodes the configured base64 secret into an HMAC {@link SecretKey},
     * caching the result for subsequent calls.
     *
     * @return the signing key used for both signing and verification
     */
    public SecretKey getSigningKey() {
        if (cachedSigningKey == null) {
            cachedSigningKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        }
        return cachedSigningKey;
    }
}
