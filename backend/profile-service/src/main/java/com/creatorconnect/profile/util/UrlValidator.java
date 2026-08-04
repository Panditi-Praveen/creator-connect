package com.creatorconnect.profile.util;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;

/**
 * Validates that a string is an absolute {@code http://} or {@code https://}
 * URL with a non-empty host.
 *
 * <p>Strategy:
 * <ol>
 *   <li>{@code null} / blank values are valid — optional fields are governed
 *       by their own constraints.</li>
 *   <li>The value must parse as a {@link URI}.</li>
 *   <li>The scheme must be {@code http} or {@code https} (case-insensitive).</li>
 *   <li>The host must be present (rules out values like {@code "https://"}).</li>
 * </ol>
 *
 * <p>Rejects strings such as {@code "ftp://x.com"}, {@code "not-a-url"}, or
 * {@code "https://"} while accepting {@code "https://example.com/path"}.
 */
public class UrlValidator implements ConstraintValidator<ValidUrl, String> {

    /**
     * Checks the value against the URL rules described in the class docs.
     *
     * @param value   the string to validate (may be {@code null})
     * @param context validation context (unused)
     * @return {@code true} when the value is {@code null}, blank, or a valid
     *         {@code http(s)} URL
     */
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true;
        }
        try {
            URI uri = new URI(value.trim());
            String scheme = uri.getScheme() == null ? null : uri.getScheme().toLowerCase(Locale.ROOT);
            return ("http".equals(scheme) || "https".equals(scheme))
                    && uri.getHost() != null && !uri.getHost().isBlank();
        } catch (URISyntaxException ex) {
            return false;
        }
    }
}
