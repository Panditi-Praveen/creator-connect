package com.creatorconnect.profile.service;

import com.creatorconnect.profile.dto.request.ProfileRequest;
import com.creatorconnect.profile.dto.request.UpdateProfileRequest;
import com.creatorconnect.profile.dto.response.ProfileResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
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
     * Loads every profile in the platform (the unified creator/freelancer
     * talent pool).
     *
     * <p>Read-only helper used by the AI Service for talent discovery.
     *
     * @return all profiles as response projections
     */
    List<ProfileResponse> getFreelancerProfiles();

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

    /**
     * Uploads or replaces the authenticated user's profile picture.
     *
     * @param authenticatedUserId the caller's id (from the JWT)
     * @param file                the image file to upload
     * @return the updated profile projection with the new image URL
     * @throws com.creatorconnect.profile.exception.ProfileNotFoundException
     *         when the user has no profile
     * @throws com.creatorconnect.profile.exception.InvalidFileException
     *         when the file fails validation
     */
    ProfileResponse uploadProfilePicture(UUID authenticatedUserId, MultipartFile file);

    /**
     * Saves or updates the authenticated user's location details.
     *
     * @param authenticatedUserId the caller's id (from the JWT)
     * @param latitude            the geographic latitude
     * @param longitude           the geographic longitude
     * @param city                optional city name
     * @param state               optional state/province name
     * @param country             optional country name
     * @param formattedAddress    optional formatted address string
     * @return the updated profile projection with the new location data
     * @throws com.creatorconnect.profile.exception.ProfileNotFoundException
     *         when the user has no profile
     */
    ProfileResponse updateLocation(UUID authenticatedUserId, Double latitude, Double longitude,
                                   String city, String state, String country, String formattedAddress);
}
