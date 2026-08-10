package com.creatorconnect.project.feign;

import feign.FeignException;
import feign.Request;
import feign.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ProfileClientService} — the best-effort Profile
 * Service façade that unwraps the success envelope and degrades gracefully
 * (never throws) when a profile cannot be resolved.
 */
@ExtendWith(MockitoExtension.class)
class ProfileClientServiceTest {

    private static final UUID USER_ID = UUID.fromString("7b092f57-a53d-46dd-b2e0-4c8f0289fb91");

    @Mock
    private ProfileClient profileClient;

    @Test
    void getProfile_whenProfileExists_returnsUnwrappedProfile() {
        ProfileResponse profile = new ProfileResponse(USER_ID, "Praveen", "Kumar",
                "Video Editor", "http://cdn.example.com/praveen.jpg", "After Effects, Premiere Pro");
        when(profileClient.getProfile(USER_ID))
                .thenReturn(new ProfileApiResponse<>(null, 200, "Profile retrieved successfully",
                        profile, "/profile/" + USER_ID));

        Optional<ProfileResponse> result = new ProfileClientService(profileClient).getProfile(USER_ID);

        assertThat(result).isPresent();
        assertThat(result.get().getUserId()).isEqualTo(USER_ID);
        assertThat(result.get().getFirstName()).isEqualTo("Praveen");
        assertThat(result.get().getSkills()).isEqualTo("After Effects, Premiere Pro");
    }

    @Test
    void getProfile_whenProfileServiceAnswers404_returnsEmpty() {
        when(profileClient.getProfile(USER_ID)).thenThrow(notFoundException());

        Optional<ProfileResponse> result = new ProfileClientService(profileClient).getProfile(USER_ID);

        assertThat(result).isEmpty();
    }

    @Test
    void getProfile_whenPayloadIsEmpty_returnsEmpty() {
        when(profileClient.getProfile(USER_ID))
                .thenReturn(new ProfileApiResponse<>(null, 200, "ok", null, "/profile/" + USER_ID));

        Optional<ProfileResponse> result = new ProfileClientService(profileClient).getProfile(USER_ID);

        assertThat(result).isEmpty();
    }

    @Test
    void getProfile_whenProfileServiceUnavailable_returnsEmpty() {
        when(profileClient.getProfile(USER_ID)).thenThrow(serviceUnavailableException());

        Optional<ProfileResponse> result = new ProfileClientService(profileClient).getProfile(USER_ID);

        assertThat(result).isEmpty();
    }

    @Test
    void getProfile_whenNoInstancesAvailable_returnsEmpty() {
        // A fully-down Profile Service (not registered with Eureka) makes the
        // LoadBalancer throw a plain RuntimeException (IllegalStateException
        // "No instances available") before any Feign request is attempted.
        // This is NOT a FeignException, so the façade must still degrade.
        when(profileClient.getProfile(USER_ID))
                .thenThrow(new IllegalStateException("No instances available for profile-service"));

        Optional<ProfileResponse> result = new ProfileClientService(profileClient).getProfile(USER_ID);

        assertThat(result).isEmpty();
    }

    /**
     * Builds a realistic {@code FeignException.NotFound} exactly as Feign
     * would after the Profile Service answers {@code 404}.
     *
     * @return the Feign {@code 404} exception
     */
    private FeignException.NotFound notFoundException() {
        Request request = Request.create(Request.HttpMethod.GET,
                "http://localhost:8082/profile/" + USER_ID,
                Map.of(), null, StandardCharsets.UTF_8);
        Response response = Response.builder()
                .status(404)
                .reason("Not Found")
                .request(request)
                .headers(Map.of())
                .build();
        return (FeignException.NotFound) FeignException.errorStatus("ProfileClient#getProfile(UUID)", response);
    }

    /**
     * Builds a {@code FeignException.ServiceUnavailable} as Feign would after
     * the Profile Service answers {@code 503} (or fails to connect).
     *
     * @return the Feign {@code 503} exception
     */
    private FeignException.ServiceUnavailable serviceUnavailableException() {
        Request request = Request.create(Request.HttpMethod.GET,
                "http://localhost:8082/profile/" + USER_ID,
                Map.of(), null, StandardCharsets.UTF_8);
        Response response = Response.builder()
                .status(503)
                .reason("Service Unavailable")
                .request(request)
                .headers(Map.of())
                .build();
        return (FeignException.ServiceUnavailable) FeignException.errorStatus("ProfileClient#getProfile(UUID)", response);
    }
}
