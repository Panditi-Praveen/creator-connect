package com.creatorconnect.profile.service;

import com.creatorconnect.profile.dto.request.ProfileRequest;
import com.creatorconnect.profile.dto.request.UpdateProfileRequest;
import com.creatorconnect.profile.dto.response.ProfileResponse;

import java.util.UUID;

/**
 * Profile Service use cases — the business logic contract layer.
 *
 * <p>Exposes the operations the Profile Service API supports. Implementations
 * live in {@code service.impl}; the interface decouples the controller from
 * concrete logic (SOLID — dependency inversion).
 *
 * <p>Ownership discipline: the {@code authenticatedUserId} parameters are
 * always derived from the JWT, never from the request body, so a caller can
 * only create/update/delete their own profile.
 */
public interface ProfileService {

    /**
     * Creates a profile for the given authenticated user.
     *
     * @param userId  the owning user's id (from the JWT)
     * @param request the validated create payload
     * @return the persisted profile projection
     * @throws com.creatorconnect.profile.exception.ProfileAlreadyExistsException
     *         when the user already owns a profile
     */
    ProfileResponse createProfile(UUID userId, ProfileRequest request);

    /**
     * Loads the profile of the given user (any authenticated caller may view
     * any profile).
     *
     * @param userId the owning user's id
     * @return the persisted profile projection
     * @throws com.creatorconnect.profile.exception.ProfileNotFoundException
     *         when the user has no profile
     */
    ProfileResponse getProfileByUserId(UUID userId);

    /**
     * Updates the profile of the given target user.
     *
     * @param authenticatedUserId the caller's id (from the JWT)
     * @param targetUserId        the owner of the profile to update
     * @param request             the validated partial-update payload
     * @return the updated profile projection
     * @throws com.creatorconnect.profile.exception.ProfileNotFoundException
     *         when the target user has no profile
     * @throws com.creatorconnect.profile.exception.ProfileAccessDeniedException
     *         when the caller is not the profile owner
     */
    ProfileResponse updateProfile(UUID authenticatedUserId, UUID targetUserId, UpdateProfileRequest request);

    /**
     * Deletes the profile of the given target user.
     *
     * @param authenticatedUserId the caller's id (from the JWT)
     * @param targetUserId        the owner of the profile to delete
     * @throws com.creatorconnect.profile.exception.ProfileNotFoundException
     *         when the target user has no profile
     * @throws com.creatorconnect.profile.exception.ProfileAccessDeniedException
     *         when the caller is not the profile owner
     */
    void deleteProfile(UUID authenticatedUserId, UUID targetUserId);
}
