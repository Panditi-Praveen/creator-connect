package com.creatorconnect.auth.security;

import com.creatorconnect.auth.entity.User;
import com.creatorconnect.auth.repository.UserRepository;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

/**
 * {@link AuthenticationProvider} that authenticates email + password
 * credentials against the {@code users} table.
 *
 * <p><b>Day 5 scope:</b> prepared but <em>not yet registered</em> with an
 * {@code AuthenticationManager}. It exists so Day 6 can plug username/password
 * authentication into the filter chain; the login endpoint currently performs
 * the same checks directly in the service layer.
 *
 * <p>Follows the classic DAO pattern:
 * <ol>
 *   <li>Loads the user by (lowercase) email — unknown emails yield a generic
 *       {@link BadCredentialsException}.</li>
 *   <li>Rejects disabled accounts with {@link DisabledException}.</li>
 *   <li>Verifies the raw password against the stored BCrypt hash.</li>
 *   <li>Returns an authenticated token carrying the user's role authority.</li>
 * </ol>
 */
@Component
public class JwtAuthenticationProvider implements AuthenticationProvider {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Creates the provider with its collaborators.
     *
     * @param userRepository  the user data access layer
     * @param passwordEncoder the BCrypt verifier
     */
    public JwtAuthenticationProvider(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String email = authentication.getName().trim().toLowerCase(Locale.ROOT);
        String rawPassword = (String) authentication.getCredentials();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        if (!user.isEnabled()) {
            throw new DisabledException("Account is disabled");
        }

        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            throw new BadCredentialsException("Invalid email or password");
        }

        return new UsernamePasswordAuthenticationToken(
                user.getEmail(),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
        );
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
