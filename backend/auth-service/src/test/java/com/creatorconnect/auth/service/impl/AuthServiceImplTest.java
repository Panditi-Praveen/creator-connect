package com.creatorconnect.auth.service.impl;

import com.creatorconnect.auth.dto.request.LoginRequest;
import com.creatorconnect.auth.dto.request.RegisterRequest;
import com.creatorconnect.auth.dto.response.LoginResponse;
import com.creatorconnect.auth.dto.response.RegisterResponse;
import com.creatorconnect.auth.entity.Role;
import com.creatorconnect.auth.entity.User;
import com.creatorconnect.auth.exception.EmailAlreadyExistsException;
import com.creatorconnect.auth.exception.InvalidCredentialsException;
import com.creatorconnect.auth.exception.UserNotFoundException;
import com.creatorconnect.auth.mapper.UserMapper;
import com.creatorconnect.auth.repository.UserRepository;
import com.creatorconnect.auth.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link AuthServiceImpl} — the Day 5 login flow plus the
 * existing registration flow, using mocked collaborators.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    private static final String EMAIL = "praveen@gmail.com";
    private static final String PASSWORD = "Password@123";
    private static final UUID USER_ID = UUID.fromString("7b092f57-a53d-46dd-b2e0-4c8f0289fb91");

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserMapper userMapper;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthServiceImpl authService;

    @Test
    void login_withValidCredentials_returnsTokenAndUserProjection() {
        User user = user(EMAIL, true);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(PASSWORD, user.getPassword())).thenReturn(true);
        when(jwtService.generateToken(user)).thenReturn("jwt-token");
        when(jwtService.getExpirationSeconds()).thenReturn(86_400L);

        LoginResponse response = authService.login(loginRequest(EMAIL, PASSWORD));

        assertThat(response.getAccessToken()).isEqualTo("jwt-token");
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        assertThat(response.getExpiresIn()).isEqualTo(86_400L);
        assertThat(response.getUserId()).isEqualTo(USER_ID);
        assertThat(response.getEmail()).isEqualTo(EMAIL);
        assertThat(response.getRole()).isEqualTo(Role.CREATOR);
    }

    @Test
    void login_normalizesEmailToLowercaseBeforeLookup() {
        User user = user(EMAIL, true);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(PASSWORD, user.getPassword())).thenReturn(true);
        when(jwtService.generateToken(user)).thenReturn("jwt-token");
        when(jwtService.getExpirationSeconds()).thenReturn(86_400L);

        authService.login(loginRequest("  PRAVEEN@GMAIL.COM ", PASSWORD));

        verify(userRepository).findByEmail(EMAIL);
    }

    @Test
    void login_withUnknownEmail_throwsUserNotFound() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(loginRequest(EMAIL, PASSWORD)))
                .isInstanceOf(UserNotFoundException.class);
        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }

    @Test
    void login_withWrongPassword_throwsInvalidCredentials() {
        User user = user(EMAIL, true);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(PASSWORD, user.getPassword())).thenReturn(false);

        assertThatThrownBy(() -> authService.login(loginRequest(EMAIL, PASSWORD)))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid email or password");
        verify(jwtService, never()).generateToken(any(User.class));
    }

    @Test
    void login_withDisabledAccount_throwsInvalidCredentials() {
        User user = user(EMAIL, false);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login(loginRequest(EMAIL, PASSWORD)))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Account is disabled");
        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }

    @Test
    void register_rejectsDuplicateEmail() {
        when(userRepository.existsByEmail(EMAIL)).thenReturn(true);

        RegisterRequest request = RegisterRequest.builder()
                .firstName("Praveen")
                .lastName("Kumar")
                .email(EMAIL)
                .password(PASSWORD)
                .role(Role.CREATOR)
                .build();

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(EmailAlreadyExistsException.class);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void register_persistsAndReturnsSafeProjection() {
        User user = user(EMAIL, true);
        RegisterRequest request = RegisterRequest.builder()
                .firstName("Praveen")
                .lastName("Kumar")
                .email(EMAIL)
                .password(PASSWORD)
                .role(Role.CREATOR)
                .build();

        when(userRepository.existsByEmail(EMAIL)).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$encoded-hash");
        when(userMapper.toEntity(any(RegisterRequest.class), anyString(), anyString()))
                .thenReturn(user);
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toResponse(user)).thenReturn(
                RegisterResponse.builder()
                        .id(USER_ID)
                        .email(EMAIL)
                        .role(Role.CREATOR)
                        .build()
        );

        RegisterResponse response = authService.register(request);

        assertThat(response.getId()).isEqualTo(USER_ID);
        assertThat(response.getEmail()).isEqualTo(EMAIL);
    }

    private LoginRequest loginRequest(String email, String password) {
        LoginRequest request = new LoginRequest();
        request.setEmail(email);
        request.setPassword(password);
        return request;
    }

    private User user(String email, boolean enabled) {
        return User.builder()
                .id(USER_ID)
                .email(email)
                .password("$2a$10$encoded-hash")
                .role(Role.CREATOR)
                .enabled(enabled)
                .build();
    }
}
