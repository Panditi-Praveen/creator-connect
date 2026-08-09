package com.creatorconnect.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.server.mvc.filter.BeforeFilterFunctions;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.servlet.function.ServerRequest;

import java.util.List;
import java.util.UUID;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the exact {@code rewritePath} configuration applied to the
 * gateway's hiring-service route in {@code application.yml}:
 *
 * <pre>
 *   regexp:     /hiring/(.*)
 *   replacement: /$1
 * </pre>
 *
 * <p>The Hiring Service's controllers are mapped at {@code /applications} and
 * {@code /reviews} (no {@code /hiring} prefix), so the gateway must strip the
 * prefix before forwarding — otherwise every {@code /hiring/**} request would
 * reach the service with an unmatched path and answer {@code 404}. These tests
 * pin the rewrite behaviour directly against the framework function the route
 * configures, including a nested path (reviews) with a UUID segment.
 *
 * <p>Assertions use {@code uri().getPath()}: that is the URI the gateway
 * forwards to (the filter rebuilds the request's URI); {@code path()} would
 * still report the original servlet-request path.
 */
class HiringRouteRewriteFilterTest {

    private static final String REGEXP = "/hiring/(.*)";
    private static final String REPLACEMENT = "/$1";

    @Test
    void rewritePath_stripsHiringPrefixFromSimplePath() {
        assertThat(rewrite().apply(request("/hiring/applications")).uri().getPath())
                .isEqualTo("/applications");
    }

    @Test
    void rewritePath_stripsHiringPrefixFromNestedPath() {
        UUID freelancerId = UUID.fromString("7b092f57-a53d-46dd-b2e0-4c8f0289fb91");

        assertThat(rewrite().apply(request("/hiring/reviews/freelancer/" + freelancerId)).uri().getPath())
                .isEqualTo("/reviews/freelancer/" + freelancerId);
    }

    @Test
    void rewritePath_doesNotMatchUnrelatedPaths() {
        assertThat(rewrite().apply(request("/applications")).uri().getPath())
                .isEqualTo("/applications");
    }

    @Test
    void rewritePath_preservesQueryString() {
        // The paginated endpoints take query params (page/size), so the
        // rewrite must strip the prefix without dropping the query string.
        ServerRequest rewritten = rewrite().apply(request("/hiring/applications/my", "page=0&size=20"));

        assertThat(rewritten.uri().getPath()).isEqualTo("/applications/my");
        assertThat(rewritten.uri().getQuery()).isEqualTo("page=0&size=20");
    }

    /**
     * Builds the exact rewrite function the gateway route configures.
     *
     * @return the configured rewritePath filter function
     */
    private Function<ServerRequest, ServerRequest> rewrite() {
        return BeforeFilterFunctions.rewritePath(REGEXP, REPLACEMENT);
    }

    /**
     * Wraps a raw request path in a WebMVC {@link ServerRequest}, the same
     * type the gateway filter operates on.
     *
     * @param path the request path (e.g. {@code /hiring/applications})
     * @return the wrapped request
     */
    private ServerRequest request(String path) {
        return request(path, null);
    }

    /**
     * Wraps a raw request path (and optional query string) in a WebMVC
     * {@link ServerRequest}.
     *
     * @param path   the request path (e.g. {@code /hiring/applications})
     * @param query  the query string (e.g. {@code page=0&size=20}), or
     *               {@code null} for none
     * @return the wrapped request
     */
    private ServerRequest request(String path, String query) {
        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        servletRequest.setRequestURI(path);
        servletRequest.setQueryString(query);
        return ServerRequest.create(servletRequest, List.of());
    }
}
