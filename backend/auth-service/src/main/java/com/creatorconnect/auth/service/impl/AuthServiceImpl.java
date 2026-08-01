package com.creatorconnect.auth.service.impl;

import com.creatorconnect.auth.dto.request.RegisterRequest;
import com.creatorconnect.auth.dto.response.RegisterResponse;
import com.creatorconnect.auth.entity.User;
import com.creatorconnect.auth.exception.EmailAlreadyExistsException;
import com.creatorconnect.auth.mapper.UserMapper;
import com.creatorconnect.auth.repository.UserRepository;
import com.creatorconnect.auth.service.AuthService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

/**
 * Concrete {@link AuthService} implementation.
 *
 * <p>Owns the Day 4 registration flow:
 * <ol>
 *   <li>Normalizes the email to lowercase (single canonical form in DB).</li>
 *   <li>Rejects duplicate emails with {@link EmailAlreadyExistsException}.</li>
 *   <li>Hashes the raw password with the injected {@link PasswordEncoder}
 *       (BCrypt).</li>
 *   <li>Maps the DTO to a {@link User} entity and persists it.</li>
 *   <li>Maps the persisted entity back to a safe {@link RegisterResponse}.</li>
 * </ol>
 *
 * <p>Dependencies are injected through the constructor only (no field
 * injection). The duplicate check and the insert happen inside one
 * {@code @Transactional} boundary, so a failure rolls back cleanly.
 */
@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    /**
     * Creates the service with its collaborators.
     *
     * @param userRepository  the user data access layer
     * @param passwordEncoder the BCrypt password hasher
     * @param userMapper      the entity/DTO mapper
     */
    public AuthServiceImpl(UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           UserMapper userMapper) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.userMapper = userMapper;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);

        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyExistsException("Email is already registered: " + email);
        }

        String encodedPassword = passwordEncoder.encode(request.getPassword());
        User user = userRepository.save(userMapper.toEntity(request, email, encodedPassword));
        return userMapper.toResponse(user);
    }
}
