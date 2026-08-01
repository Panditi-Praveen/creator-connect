package com.creatorconnect.auth.repository;

import com.creatorconnect.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for the {@link User} entity.
 *
 * <p>Provides CRUD access plus the two derived queries required by the
 * registration flow:
 * <ul>
 *   <li>{@link #findByEmail(String)} — loads a user by its email address
 *       (used by login and duplicate checks).</li>
 *   <li>{@link #existsByEmail(String)} — cheap existence check used to reject
 *       duplicate registrations before insert.</li>
 * </ul>
 *
 * <p>Emails are normalized to lowercase by the service layer before any query
 * is executed, so lookups are case-consistent with the {@code unique} index on
 * {@code users.email}.
 */
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Finds a user by its (lowercase-normalized) email address.
     *
     * @param email the email address to look up
     * @return the matching user wrapped in an {@link Optional}, or
     *         {@link Optional#empty()} when no user exists
     */
    Optional<User> findByEmail(String email);

    /**
     * Returns whether a user with the given (lowercase-normalized) email
     * address already exists.
     *
     * @param email the email address to check
     * @return {@code true} when at least one user owns the email
     */
    boolean existsByEmail(String email);
}
