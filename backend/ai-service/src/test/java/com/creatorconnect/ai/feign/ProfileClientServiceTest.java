package com.creatorconnect.ai.feign;

import com.creatorconnect.ai.exception.ProfileServiceUnavailableException;
import feign.FeignException;
import feign.Request;
import feign.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ProfileClientService} — the façade that turns the
 * Profile Service Feign call into the AI Service's own controlled exception.
 * A Profile Service outage must surface as a clean
 * {@link ProfileServiceUnavailableException} (mapped to {@code 503}), never
 * as a raw stack trace.
 */
@ExtendWith(MockitoExtension.class)
class ProfileClientServiceTest {

    private static final UUID PROFILE_ID = UUID.fromString("8dade014-d0c9-470d-acb8-744a3712fc76");
    private static final UUID USER_ID = UUID.fromString("f64af15f-01e6-47ff-9d0c-06d55ee21f46");

    @Mock
    private ProfileClient profileClient;

    @Test
    void fetchFreelancerProfiles_whenProfilesReturned_returnsUnwrappedList() {
        ProfileResponse profile = new ProfileResponse(
                PROFILE_ID, USER_ID, "Alex", "Rivera", "Senior Video Editor",
                null, null, "Mumbai, India", null, null, null,
                "Video Editing, After Effects", 6, true, null, null);
        when(profileClient.getFreelancerProfiles())
                .thenReturn(new ProfileApiResponse<>(LocalDateTime.now(), 200,
                        "Profiles retrieved successfully",
                        List.of(profile), "/profile/freelancers"));

        List<ProfileResponse> result = new ProfileClientService(profileClient).fetchFreelancerProfiles();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(PROFILE_ID);
        assertThat(result.get(0).getFirstName()).isEqualTo("Alex");
        assertThat(result.get(0).getSkills()).isEqualTo("Video Editing, After Effects");
    }

    @Test
    void fetchFreelancerProfiles_whenPayloadIsNull_returnsEmptyList() {
        when(profileClient.getFreelancerProfiles())
                .thenReturn(new ProfileApiResponse<>(LocalDateTime.now(), 200,
                        "Profiles retrieved successfully", null, "/profile/freelancers"));

        List<ProfileResponse> result = new ProfileClientService(profileClient).fetchFreelancerProfiles();

        assertThat(result).isEmpty();
    }

    @Test
    void fetchFreelancerProfiles_whenResponseIsNull_returnsEmptyList() {
        when(profileClient.getFreelancerProfiles()).thenReturn(null);

        List<ProfileResponse> result = new ProfileClientService(profileClient).fetchFreelancerProfiles();

        assertThat(result).isEmpty();
    }

    @Test
    void fetchFreelancerProfiles_whenProfileServiceDown_throwsProfileServiceUnavailable() {
        when(profileClient.getFreelancerProfiles()).thenThrow(serviceUnavailableException());

        assertThatThrownBy(() -> new ProfileClientService(profileClient).fetchFreelancerProfiles())
                .isInstanceOf(ProfileServiceUnavailableException.class)
                .hasMessage("The Profile Service is temporarily unavailable");
    }

    /**
     * Builds a {@code FeignException.ServiceUnavailable} as Feign would after
     * the Profile Service answers {@code 503} (or fails to connect).
     *
     * @return the Feign {@code 503} exception
     */
    private FeignException.ServiceUnavailable serviceUnavailableException() {
        Request request = Request.create(Request.HttpMethod.GET,
                "http://localhost:8082/profile/freelancers",
                Map.of(), null, StandardCharsets.UTF_8);
        Response response = Response.builder()
                .status(503)
                .reason("Service Unavailable")
                .request(request)
                .headers(Map.of())
                .build();
        return (FeignException.ServiceUnavailable) FeignException.errorStatus(
                "ProfileClient#getFreelancerProfiles()", response);
    }
}
