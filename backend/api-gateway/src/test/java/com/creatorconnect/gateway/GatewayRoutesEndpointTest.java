package com.creatorconnect.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies the gateway actuator surface when the Spring Cloud Gateway Server
 * WebMvc gateway is enabled.
 *
 * <p>The WebMvc gateway (Spring Cloud 2025.x) does not ship the classic
 * {@code /actuator/gateway/routes} endpoint, so the project provides a custom
 * one ({@code GatewayRoutesEndpoint}). These tests prove the endpoint is
 * reachable and reports the routes declared in application.yml.
 */
@SpringBootTest(properties = {
        "eureka.client.enabled=false",
        "server.port=0"
})
@AutoConfigureMockMvc
class GatewayRoutesEndpointTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void gatewayRoutesEndpointReturnsAllConfiguredRoutes() throws Exception {
        mockMvc.perform(get("/actuator/gateway/routes"))
                .andExpect(status().isOk())
                // Presence-based (order/count independent): adding or reordering
                // routes in application.yml must not break this test.
                .andExpect(jsonPath("$.routes[*].id", hasItems(
                        "auth-service", "profile-service", "project-service",
                        "hiring-service", "ai-service")))
                .andExpect(jsonPath("$.routes[?(@.id == 'auth-service')].uri",
                        hasItem("lb://auth-service")))
                .andExpect(jsonPath("$..predicates[?(@.name == 'Path')]").isNotEmpty());
    }

    @Test
    void healthEndpointIsAvailable() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    void infoEndpointIsAvailable() throws Exception {
        mockMvc.perform(get("/actuator/info"))
                .andExpect(status().isOk());
    }
}
