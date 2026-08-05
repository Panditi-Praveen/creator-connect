package com.creatorconnect.gateway.actuator;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.endpoint.web.annotation.RestControllerEndpoint;
import org.springframework.cloud.gateway.server.mvc.config.FilterProperties;
import org.springframework.cloud.gateway.server.mvc.config.GatewayMvcProperties;
import org.springframework.cloud.gateway.server.mvc.config.PredicateProperties;
import org.springframework.cloud.gateway.server.mvc.config.RouteProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Actuator endpoint exposing the routes configured for the Spring Cloud Gateway
 * <b>Server WebMvc</b> (MVC) gateway.
 *
 * <p>Why this exists: the classic reactive (WebFlux) gateway ships a built-in
 * {@code /actuator/gateway/routes} endpoint backed by {@code GatewayControllerEndpoint}.
 * The MVC variant ({@code spring-cloud-gateway-server-webmvc}, introduced in
 * Spring Cloud 2025.0.x) provides no gateway actuator endpoint at all — a request
 * to {@code /actuator/gateway/routes} returned {@code 404 Not Found} even though
 * the routes were configured and working.
 *
 * <p>This endpoint restores that capability by reading the gateway's bound
 * {@link GatewayMvcProperties} bean (the object backing
 * {@code spring.cloud.gateway.server.webmvc.routes} in {@code application.yml})
 * and returning the configured routes in a JSON shape that mirrors the classic
 * gateway's response: each route carries its id, destination URI, order,
 * predicates and filters.
 *
 * <p>It is served on {@code /actuator/gateway/routes} because the endpoint id
 * {@code gateway} is already whitelisted in
 * {@code management.endpoints.web.exposure.include: health,info,gateway,env,metrics}.
 * No YAML change is required.
 *
 * <p>Note: this endpoint reports the routes <b>configured</b> under
 * {@code spring.cloud.gateway.server.webmvc.routes} (the bound
 * {@link GatewayMvcProperties}), which is the source of truth for the MVC
 * gateway's route definitions. It is an administrative/observability listing —
 * it does not reflect runtime request routing (e.g. which instances a
 * {@code lb://} URI resolved to at request time).
 *
 * <p>{@code @Component} is required so the actuator's endpoint discoverer
 * finds the class via component scanning (verified empirically: without it the
 * endpoint is never registered and the path returns 404).
 */
@Component
@RestControllerEndpoint(id = "gateway")
public class GatewayRoutesEndpoint {

    private final ObjectProvider<GatewayMvcProperties> gatewayMvcProperties;

    /**
     * Creates the endpoint. An {@link ObjectProvider} is used (rather than a plain
     * constructor dependency) so the endpoint still loads when the gateway is
     * disabled (e.g. {@code spring.cloud.gateway.server.webmvc.enabled=false} in
     * tests) — in that case it simply reports an empty route list.
     *
     * @param gatewayMvcProperties the gateway's bound configuration properties
     */
    public GatewayRoutesEndpoint(ObjectProvider<GatewayMvcProperties> gatewayMvcProperties) {
        this.gatewayMvcProperties = gatewayMvcProperties;
    }

    /**
     * GET /actuator/gateway/routes — returns every route defined under
     * {@code spring.cloud.gateway.server.webmvc.routes}.
     *
     * <p>A {@code RestControllerEndpoint} is a plain Spring MVC controller: the
     * operation is exposed via {@link GetMapping} (actuator operation
     * annotations such as {@code @ReadOperation} are not allowed here), and the
     * endpoint id {@code gateway} provides the {@code /actuator/gateway} base
     * path.
     *
     * @return a map with a single {@code routes} key holding the route list
     */
    @GetMapping("/routes")
    public Map<String, Object> routes() {
        GatewayMvcProperties properties = gatewayMvcProperties.getIfAvailable();
        if (properties == null) {
            return Map.of("routes", List.of());
        }
        List<Map<String, Object>> routes = properties.getRoutes().stream()
                .map(this::toRouteMap)
                .toList();
        return Map.of("routes", routes);
    }

    /**
     * Converts a bound {@link RouteProperties} into a JSON-friendly map.
     *
     * @param route the route properties from configuration
     * @return a map with id, uri, order, predicates and filters
     */
    private Map<String, Object> toRouteMap(RouteProperties route) {
        Map<String, Object> routeMap = new LinkedHashMap<>();
        routeMap.put("id", route.getId());
        routeMap.put("uri", route.getUri() != null ? route.getUri().toString() : null);
        routeMap.put("order", route.getOrder());
        routeMap.put("predicates", toPredicateList(route.getPredicates() == null ? List.of() : route.getPredicates()));
        routeMap.put("filters", toFilterList(route.getFilters() == null ? List.of() : route.getFilters()));
        return routeMap;
    }

    /**
     * Converts a list of predicates into a list of {@code {"name": ..., "args": {...}}} maps.
     */
    private List<Map<String, Object>> toPredicateList(List<PredicateProperties> predicates) {
        return predicates.stream().map(predicate -> {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("name", predicate.getName());
            entry.put("args", predicate.getArgs());
            return entry;
        }).toList();
    }

    /**
     * Converts a list of filters into a list of {@code {"name": ..., "args": {...}}} maps.
     */
    private List<Map<String, Object>> toFilterList(List<FilterProperties> filters) {
        return filters.stream().map(filter -> {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("name", filter.getName());
            entry.put("args", filter.getArgs());
            return entry;
        }).toList();
    }
}
