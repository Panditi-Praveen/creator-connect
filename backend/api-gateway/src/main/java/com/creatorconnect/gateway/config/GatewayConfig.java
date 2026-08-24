package com.creatorconnect.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.filter.CommonsRequestLoggingFilter;

import java.util.List;

/**
 * Gateway configuration.
 *
 * <p>Routes are defined in application.yml under
 * {@code spring.cloud.gateway.server.webmvc.routes}. This class provides
 * supplementary Spring beans:
 * <ul>
 *   <li>{@link CorsFilter} — allows the frontend origin to call the gateway
 *       with credentials, standard methods, and required headers.</li>
 *   <li>{@link CommonsRequestLoggingFilter} — logs every incoming request
 *       with client info and query string.</li>
 * </ul>
 */
@Configuration
public class GatewayConfig {

    /**
     * CORS filter that allows the frontend to call the gateway.
     *
     * <p>In development the Vite dev server proxies API calls so the browser
     * never hits CORS. In production (or when {@code VITE_API_BASE_URL} points
     * to a different origin) the browser sends preflight requests that this
     * filter answers.
     *
     * <p>Allowed origins are read from {@code CORS_ALLOWED_ORIGINS} env var
     * (comma-separated). When not set, defaults to {@code http://localhost:5173}
     * (the Vite dev server) and {@code http://localhost:3000} (a common prod
     * port).
     */
    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();

        String envOrigins = System.getenv("CORS_ALLOWED_ORIGINS");
        if (envOrigins != null && !envOrigins.isBlank()) {
            for (String origin : envOrigins.split(",")) {
                String trimmed = origin.trim();
                if (!trimmed.isEmpty()) {
                    config.addAllowedOrigin(trimmed);
                }
            }
        } else {
            config.addAllowedOrigin("http://localhost:5173");
            config.addAllowedOrigin("http://localhost:3000");
        }

        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }

    /**
     * Logs every incoming request with client info and query string.
     * Uses Spring MVC native CommonsRequestLoggingFilter — no WebFlux/Reactor dependency.
     *
     * To also log headers and payload, set the corresponding flags to true.
     *
     * The log level must be set to DEBUG for this filter to output logs:
     *   logging.level.org.springframework.web.filter.CommonsRequestLoggingFilter: DEBUG
     */
    @Bean
    public CommonsRequestLoggingFilter requestLoggingFilter() {
        CommonsRequestLoggingFilter filter = new CommonsRequestLoggingFilter();
        filter.setIncludeClientInfo(true);
        filter.setIncludeQueryString(true);
        filter.setIncludeHeaders(false);
        filter.setIncludePayload(false);
        filter.setAfterMessagePrefix("GATEWAY REQUEST: ");
        return filter;
    }
}
