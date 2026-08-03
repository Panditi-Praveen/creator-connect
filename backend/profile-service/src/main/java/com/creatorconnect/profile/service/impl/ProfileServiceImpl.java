package com.creatorconnect.profile.service.impl;

import com.creatorconnect.profile.dto.request.ProfileRequest;
import com.creatorconnect.profile.dto.request.UpdateProfileRequest;
import com.creatorconnect.profile.dto.response.ProfileResponse;
import com.creatorconnect.profile.entity.Profile;
import com.creatorconnect.profile.exception.ProfileAccessDeniedException;
import com.creatorconnect.profile.exception.ProfileAlreadyExistsException;
import com.creatorconnect.profile.exception.ProfileNotFoundException;
import com.creatorconnect.profile.mapper.ProfileMapper;
import com.creatorconnect.profile.repository.ProfileRepository;
import com.creatorconnect.profile.service.ProfileService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Concrete {@link ProfileService} implementation.
 *
 * <p>Owns the profile lifecycle with these rules:
 * <ol>
 *   <li><b>Create</b> — a user may own exactly one profile; duplicates are
 *       rejected with {@link ProfileAlreadyExistsException}.</li>
 *   <li><b>Get</b> — any authenticated user may view any profile; missing
 *       profiles yield {@link ProfileNotFoundException}.</li>
 *   <li><b>Update / Delete</b> — the caller must be the profile owner; the
 *       check runs after the existence check so the API never leaks whether a
 *       profile exists to non-owners ({@code 404} before {@code 403}).</li>
 * </ol>
 *
 * <p>Dependencies are injected through the constructor only (no field
 * injection). Write operations run inside one {@code @Transactional} boundary
 * so a failure rolls back cleanly.
 */
@Service
public class ProfileServiceImpl implements ProfileService {

    private final ProfileRepository profileRepository;
    private final ProfileMapper profileMapper;

    /**
     * Creates the service with its collaborators.
     *
     * @param profileRepository the profile data access layer
     * @param profileMapper     the entity/DTO mapper
     */
    public ProfileServiceImpl(ProfileRepository profileRepository, ProfileMapper profileMapper) {
        this.profileRepository = profileRepository;
        this.profileMapper = profileMapper;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public ProfileResponse createProfile(UUID userId, ProfileRequest request) {
        if (profileRepository.existsByUserId(userId)) {
            throw new ProfileAlreadyExistsException("Profile already exists for user: " + userId);
        }
        Profile profile = profileRepository.save(profileMapper.toEntity(userId, request));
        return profileMapper.toResponse(profile);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public ProfileResponse getProfileByUserId(UUID userId) {
        return profileRepository.findByUserId(userId)
                .map(profileMapper::toResponse)
                .orElseThrow(() -> new ProfileNotFoundException("Profile not found for user: " + userId));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public ProfileResponse updateProfile(UUID authenticatedUserId, UUID targetUserId, UpdateProfileRequest request) {
        Profile profile = findOwnedProfile(authenticatedUserId, targetUserId);
        profileMapper.applyUpdate(profile, request);
        return profileMapper.toResponse(profileRepository.save(profile));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void deleteProfile(UUID authenticatedUserId, UUID targetUserId) {
        Profile profile = findOwnedProfile(authenticatedUserId, targetUserId);
        profileRepository.delete(profile);
    }

    /**
     * Loads the target user's profile and verifies the caller owns it.
     *
     * <p>Existence is checked first ({@code 404}) so non-owners cannot probe
     * for profiles; only then is ownership enforced ({@code 403}).
     *
     * @param authenticatedUserId the caller's id (from the JWT)
     * @param targetUserId        the owner of the requested profile
     * @return the owned profile
     * @throws ProfileNotFoundException     when the target user has no profile
     * @throws ProfileAccessDeniedException when the caller is not the owner
     */
    private Profile findOwnedProfile(UUID authenticatedUserId, UUID targetUserId) {
        Profile profile = profileRepository.findByUserId(targetUserId)
                .orElseThrow(() -> new ProfileNotFoundException("Profile not found for user: " + targetUserId));
        if (!profile.getUserId().equals(authenticatedUserId)) {
            throw new ProfileAccessDeniedException("You do not have permission to modify this profile");
        }
        return profile;
    }
}
