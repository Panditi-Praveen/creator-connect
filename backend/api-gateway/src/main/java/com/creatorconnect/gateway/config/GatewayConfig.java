package com.creatorconnect.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.CommonsRequestLoggingFilter;

/**
 * Gateway configuration.
 *
 * Routes are defined in application.yml under spring.cloud.gateway.server.webmvc.routes.
 * This class provides supplementary Spring beans such as the request logging filter.
 */
@Configuration
public class GatewayConfig {

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
