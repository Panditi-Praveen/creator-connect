package com.creatorconnect.gateway.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Adds standard HTTP security headers to every response passing through the
 * gateway. These headers protect against common web vulnerabilities:
 *
 * <ul>
 *   <li><b>X-Content-Type-Options: nosniff</b> — prevents MIME-type sniffing</li>
 *   <li><b>X-Frame-Options: DENY</b> — prevents clickjacking via iframes</li>
 *   <li><b>Referrer-Policy: strict-origin-when-cross-origin</b> — controls
 *       referer leakage</li>
 *   <li><b>X-XSS-Protection: 0</b> — disables the legacy XSS filter (modern
 *       browsers use CSP instead; the legacy filter can introduce vulnerabilities)</li>
 *   <li><b>Strict-Transport-Security</b> — only added when the request arrives
 *       over HTTPS (i.e. in production behind TLS termination)</li>
 * </ul>
 *
 * <p>Content-Security-Policy is intentionally omitted because the React SPA
 * loads inline styles and scripts that a strict CSP would break without
 * significant build-tool changes.
 */
@Component
public class SecurityHeadersFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("X-Frame-Options", "DENY");
        response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
        response.setHeader("X-XSS-Protection", "0");
        response.setHeader("Permissions-Policy", "camera=(), microphone=(), geolocation=()");

        // Only add HSTS when the request arrived over HTTPS (production behind TLS)
        if (request.isSecure()) {
            response.setHeader("Strict-Transport-Security",
                    "max-age=31536000; includeSubDomains");
        }

        filterChain.doFilter(request, response);
    }
}
