package com.creatorconnect.gateway.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Simple in-memory rate limiter for sensitive gateway endpoints.
 *
 * <p>Protects:
 * <ul>
 *   <li>{@code POST /auth/login} — 10 requests/min per IP</li>
 *   <li>{@code POST /auth/register} — 5 requests/min per IP</li>
 *   <li>{@code POST /ai/discover} — 15 requests/min per IP</li>
 * </ul>
 *
 * <p>Uses a sliding-window counter with automatic cleanup. Not suitable for
 * distributed deployments (each gateway instance has its own counters), but
 * effective for a single-instance setup.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

    /** Window size in milliseconds (1 minute). */
    private static final long WINDOW_MS = 60_000;

    /** Per-IP request limits per window for each path pattern. */
    private static final Map<String, Integer> RATE_LIMITS = Map.of(
            "/auth/login", 10,
            "/auth/register", 5,
            "/ai/discover", 15
    );

    /** Tracks request counts per key (IP + path) within the current window. */
    private final ConcurrentHashMap<String, RequestCounter> counters = new ConcurrentHashMap<>();

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        Integer limit = findLimit(path);

        if (limit != null) {
            String clientIp = getClientIp(request);
            String key = clientIp + ":" + path;
            long now = System.currentTimeMillis();

            RequestCounter counter = counters.compute(key, (k, existing) -> {
                if (existing == null || now - existing.windowStart > WINDOW_MS) {
                    return new RequestCounter(now);
                }
                return existing;
            });

            int count = counter.incrementAndGet();
            if (count > limit) {
                log.warn("Rate limit exceeded for {} on {} (count={}, limit={})", clientIp, path, count, limit);
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                Map<String, Object> body = Map.of(
                        "timestamp", java.time.Instant.now().toString(),
                        "status", 429,
                        "error", "Too Many Requests",
                        "message", "Too many requests. Please try again later.",
                        "path", path
                );
                objectMapper.writeValue(response.getWriter(), body);
                return;
            }

            // Set rate limit headers
            response.setHeader("X-RateLimit-Limit", String.valueOf(limit));
            response.setHeader("X-RateLimit-Remaining", String.valueOf(Math.max(0, limit - count)));
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Finds the rate limit for the given path, checking for exact matches
     * on the sensitive endpoints.
     */
    private Integer findLimit(String path) {
        for (Map.Entry<String, Integer> entry : RATE_LIMITS.entrySet()) {
            if (path.equals(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }

    /** Extracts the client IP, respecting X-Forwarded-For from reverse proxies. */
    private String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            // Take the first IP (the original client)
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    /** Cleanup old counters periodically to prevent memory leaks. */
    private static class RequestCounter {
        final long windowStart;
        private final AtomicInteger count = new AtomicInteger(0);

        RequestCounter(long windowStart) {
            this.windowStart = windowStart;
        }

        int incrementAndGet() {
            return count.incrementAndGet();
        }
    }
}
