package com.creatorconnect.profile.repository;

import com.creatorconnect.profile.entity.Profile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for the {@link Profile} entity.
 *
 * <p>Provides CRUD access plus the two derived queries required by the profile
 * use cases:
 * <ul>
 *   <li>{@link #findByUserId(UUID)} — loads the profile of a given user
 *       (used by get/update/delete and the {@code /profile/me} endpoint).</li>
 *   <li>{@link #existsByUserId(UUID)} — cheap existence check used to reject
 *       duplicate profile creation before insert.</li>
 * </ul>
 */
public interface ProfileRepository extends JpaRepository<Profile, UUID> {

    /**
     * Finds the profile owned by the given user.
     *
     * @param userId the owning user's id (matches the JWT {@code userId} claim)
     * @return the matching profile wrapped in an {@link Optional}, or
     *         {@link Optional#empty()} when the user has no profile
     */
    Optional<Profile> findByUserId(UUID userId);

    /**
     * Returns whether a profile for the given user already exists.
     *
     * @param userId the owning user's id
     * @return {@code true} when the user already has a profile
     */
    boolean existsByUserId(UUID userId);
}
