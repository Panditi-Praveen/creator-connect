package com.creatorconnect.ai.config;

import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Feign client configuration.
 *
 * <p>Registers a single {@link RequestInterceptor} that forwards the caller's
 * {@code Authorization: Bearer <token>} header onto every outbound Feign
 * request. This is what lets the AI Service call the Profile Service with the
 * same JWT the client presented here — the Profile Service authenticates every
 * request, so without forwarding the call would be rejected with {@code 401}.
 *
 * <p>When no servlet request is bound to the current thread (e.g. a Feign
 * call issued outside a request context in tests), the interceptor simply
 * adds no header.
 */
@Configuration
public class FeignClientConfig {

    /**
     * Copies the inbound request's {@code Authorization} header to the
     * outbound Feign request template, if present.
     *
     * @return the token-forwarding interceptor
     */
    @Bean
    public RequestInterceptor bearerTokenForwardingInterceptor() {
        return template -> {
            RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
            if (attributes instanceof ServletRequestAttributes servletRequestAttributes) {
                String authorization = servletRequestAttributes.getRequest().getHeader("Authorization");
                if (authorization != null && !authorization.isBlank()) {
                    template.header("Authorization", authorization);
                }
            }
        };
    }
}
