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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ProfileServiceImpl} — create/get/update/delete flows
 * plus the duplicate and ownership rules, using mocked collaborators.
 */
@ExtendWith(MockitoExtension.class)
class ProfileServiceImplTest {

    private static final UUID OWNER_ID = UUID.fromString("7b092f57-a53d-46dd-b2e0-4c8f0289fb91");
    private static final UUID OTHER_USER_ID = UUID.fromString("8c1a3e68-b64e-57ee-c3f1-5d9f1390ac02");
    private static final UUID PROFILE_ID = UUID.fromString("9d2b4f79-c75f-68ff-d402-6ea024a1bd13");

    @Mock
    private ProfileRepository profileRepository;

    @Mock
    private ProfileMapper profileMapper;

    @InjectMocks
    private ProfileServiceImpl profileService;

    @Test
    void createProfile_withNoExistingProfile_persistsAndReturnsResponse() {
        ProfileRequest request = profileRequest();
        Profile entity = profile(OWNER_ID);
        when(profileRepository.existsByUserId(OWNER_ID)).thenReturn(false);
        when(profileMapper.toEntity(OWNER_ID, request)).thenReturn(entity);
        when(profileRepository.save(entity)).thenReturn(entity);
        when(profileMapper.toResponse(entity)).thenReturn(profileResponse(OWNER_ID));

        ProfileResponse response = profileService.createProfile(OWNER_ID, request);

        assertThat(response.getUserId()).isEqualTo(OWNER_ID);
        assertThat(response.getFirstName()).isEqualTo("Praveen");
        verify(profileRepository).save(entity);
    }

    @Test
    void createProfile_whenProfileAlreadyExists_throwsProfileAlreadyExists() {
        when(profileRepository.existsByUserId(OWNER_ID)).thenReturn(true);

        assertThatThrownBy(() -> profileService.createProfile(OWNER_ID, profileRequest()))
                .isInstanceOf(ProfileAlreadyExistsException.class);
        verify(profileRepository, never()).save(any(Profile.class));
    }

    @Test
    void getProfileByUserId_whenFound_returnsResponse() {
        Profile entity = profile(OWNER_ID);
        when(profileRepository.findByUserId(OWNER_ID)).thenReturn(Optional.of(entity));
        when(profileMapper.toResponse(entity)).thenReturn(profileResponse(OWNER_ID));

        ProfileResponse response = profileService.getProfileByUserId(OWNER_ID);

        assertThat(response.getUserId()).isEqualTo(OWNER_ID);
    }

    @Test
    void getProfileByUserId_whenMissing_throwsProfileNotFound() {
        when(profileRepository.findByUserId(OWNER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> profileService.getProfileByUserId(OWNER_ID))
                .isInstanceOf(ProfileNotFoundException.class);
    }

    @Test
    void updateProfile_byOwner_updatesAndReturnsResponse() {
        Profile entity = profile(OWNER_ID);
        UpdateProfileRequest request = updateProfileRequest();
        when(profileRepository.findByUserId(OWNER_ID)).thenReturn(Optional.of(entity));
        when(profileRepository.save(entity)).thenReturn(entity);
        when(profileMapper.toResponse(entity)).thenReturn(profileResponse(OWNER_ID));

        ProfileResponse response = profileService.updateProfile(OWNER_ID, OWNER_ID, request);

        assertThat(response.getUserId()).isEqualTo(OWNER_ID);
        verify(profileMapper).applyUpdate(entity, request);
    }

    @Test
    void updateProfile_byNonOwner_throwsAccessDenied() {
        Profile entity = profile(OWNER_ID);
        when(profileRepository.findByUserId(OWNER_ID)).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> profileService.updateProfile(OTHER_USER_ID, OWNER_ID, updateProfileRequest()))
                .isInstanceOf(ProfileAccessDeniedException.class);
        verify(profileRepository, never()).save(any(Profile.class));
    }

    @Test
    void updateProfile_whenMissing_throwsProfileNotFound() {
        when(profileRepository.findByUserId(OWNER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> profileService.updateProfile(OWNER_ID, OWNER_ID, updateProfileRequest()))
                .isInstanceOf(ProfileNotFoundException.class);
    }

    @Test
    void deleteProfile_byOwner_deletes() {
        Profile entity = profile(OWNER_ID);
        when(profileRepository.findByUserId(OWNER_ID)).thenReturn(Optional.of(entity));

        profileService.deleteProfile(OWNER_ID, OWNER_ID);

        verify(profileRepository).delete(entity);
    }

    @Test
    void deleteProfile_byNonOwner_throwsAccessDenied() {
        Profile entity = profile(OWNER_ID);
        when(profileRepository.findByUserId(OWNER_ID)).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> profileService.deleteProfile(OTHER_USER_ID, OWNER_ID))
                .isInstanceOf(ProfileAccessDeniedException.class);
        verify(profileRepository, never()).delete(any(Profile.class));
    }

    @Test
    void deleteProfile_whenMissing_throwsProfileNotFound() {
        when(profileRepository.findByUserId(OWNER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> profileService.deleteProfile(OWNER_ID, OWNER_ID))
                .isInstanceOf(ProfileNotFoundException.class);
        verify(profileRepository, never()).delete(any(Profile.class));
    }

    private ProfileRequest profileRequest() {
        return ProfileRequest.builder()
                .firstName("Praveen")
                .lastName("Kumar")
                .headline("Senior Video Editor")
                .location("Bengaluru")
                .experience(8)
                .availableForHire(true)
                .build();
    }

    private UpdateProfileRequest updateProfileRequest() {
        return UpdateProfileRequest.builder()
                .headline("Lead Video Editor")
                .build();
    }

    private Profile profile(UUID userId) {
        return Profile.builder()
                .id(PROFILE_ID)
                .userId(userId)
                .firstName("Praveen")
                .lastName("Kumar")
                .headline("Senior Video Editor")
                .availableForHire(true)
                .build();
    }

    private ProfileResponse profileResponse(UUID userId) {
        return ProfileResponse.builder()
                .id(PROFILE_ID)
                .userId(userId)
                .firstName("Praveen")
                .lastName("Kumar")
                .availableForHire(true)
                .build();
    }
}
