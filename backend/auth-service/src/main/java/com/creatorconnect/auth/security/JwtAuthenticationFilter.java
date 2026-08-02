package com.creatorconnect.auth.security;

import com.creatorconnect.auth.entity.User;
import com.creatorconnect.auth.repository.UserRepository;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Servlet filter that authenticates requests carrying a {@code Bearer} JWT.
 *
 * <p><b>Day 5 scope:</b> this class is <em>prepared but intentionally NOT
 * active</em> yet — the login API and token issuance land first. It is kept
 * from auto-registering as a servlet filter by the disabled
 * {@code FilterRegistrationBean} in {@code config.SecurityBeansConfig}, and
 * will be wired into the {@code SecurityFilterChain} in Day 6 when protected
 * endpoints arrive.
 *
 * <p>Behaviour once enabled:
 * <ol>
 *   <li>Reads the {@code Authorization: Bearer <token>} header.</li>
 *   <li>Validates the token signature/expiry via {@link JwtService}.</li>
 *   <li>Loads the subject user and, if present and enabled, populates the
 *       {@code SecurityContext} with a {@link UsernamePasswordAuthenticationToken}
 *       carrying the user's role authority.</li>
 *   <li>Invalid or absent tokens are ignored — downstream authorization rules
 *       decide whether the request is allowed.</li>
 * </ol>
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final UserRepository userRepository;

    /**
     * Creates the filter with its collaborators.
     *
     * @param jwtService     the JWT validator
     * @param userRepository the user data access layer
     */
    public JwtAuthenticationFilter(JwtService jwtService, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            String token = header.substring(BEARER_PREFIX.length());
            authenticateIfValid(token);
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Validates the token and, on success, sets the authenticated principal.
     *
     * @param token the raw JWT from the authorization header
     */
    private void authenticateIfValid(String token) {
        try {
            if (jwtService.isValid(token)) {
                String email = jwtService.extractUsername(token);
                userRepository.findByEmail(email)
                        .filter(User::isEnabled)
                        .ifPresent(this::setAuthentication);
            }
        } catch (JwtException | IllegalArgumentException ex) {
            SecurityContextHolder.clearContext();
        }
    }

    /**
     * Stores the authenticated principal (email + role authority) in the
     * security context.
     *
     * @param user the loaded, enabled user
     */
    private void setAuthentication(User user) {
        var authentication = new UsernamePasswordAuthenticationToken(
                user.getEmail(),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
