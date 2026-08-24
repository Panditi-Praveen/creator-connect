package com.creatorconnect.auth.controller;

import com.creatorconnect.auth.config.SecurityBeansConfig;
import com.creatorconnect.auth.repository.UserRepository;
import com.creatorconnect.auth.security.JwtAuthenticationEntryPoint;
import com.creatorconnect.auth.security.JwtAuthenticationFilter;
import com.creatorconnect.auth.security.JwtService;
import com.creatorconnect.auth.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web-layer tests for {@link AuthController}.
 *
 * <p>{@code @WebMvcTest} loads only the controller slice; the production
 * security chain (which permits the auth endpoints and Swagger/actuator
 * prefixes) is imported so an unknown path under a permit-all prefix exercises
 * the real request path. The global exception handler (a
 * {@code @ControllerAdvice}) is picked up automatically by the slice.
 */
@WebMvcTest(
        value = AuthController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class)
)
@Import({SecurityBeansConfig.class, AuthControllerTest.SecurityTestConfig.class})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserRepository userRepository;

    @Test
    void unknownPath_underPermittedPrefix_returns404() throws Exception {
        mockMvc.perform(get("/v3/api-docs/does-not-exist"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Resource not found"));
    }

    /**
     * Supplies the {@link JwtAuthenticationFilter} and
     * {@link JwtAuthenticationEntryPoint} the production
     * {@link SecurityBeansConfig} references. The filter is not part of the
     * auth security chain for public endpoints, so the mocks are never
     * exercised.
     */
    @TestConfiguration
    static class SecurityTestConfig {

        @Bean
        JwtAuthenticationFilter jwtAuthenticationFilter(JwtService jwtService, UserRepository userRepository) {
            return new JwtAuthenticationFilter(jwtService, userRepository);
        }

        @Bean
        JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint(ObjectMapper objectMapper) {
            return new JwtAuthenticationEntryPoint(objectMapper);
        }
    }
}
