package com.creatorconnect.ai.feign;

import com.creatorconnect.ai.exception.ProfileServiceUnavailableException;
import feign.FeignException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Small façade over {@link ProfileClient} that turns raw Feign failures into
 * the AI Service's own controlled exception.
 *
 * <p>Keeping the Feign call — and its failure translation — in one place
 * avoids duplicating the error handling: the discovery flow only needs the
 * talent pool, and a Profile Service outage must surface as a clean
 * {@code 503} via the global exception handler rather than a stack trace.
 */
@Service
public class ProfileClientService {

    private final ProfileClient profileClient;

    /**
     * Creates the façade with its Feign client.
     *
     * @param profileClient the declarative Profile Service client
     */
    public ProfileClientService(ProfileClient profileClient) {
        this.profileClient = profileClient;
    }

    /**
     * Fetches the full talent pool.
     *
     * @return the list of profiles (empty when the Profile Service returns no
     *         payload — never {@code null})
     * @throws ProfileServiceUnavailableException when the Profile Service is
     *         unreachable or answers an error status
     */
    public List<ProfileResponse> fetchFreelancerProfiles() {
        try {
            ProfileApiResponse<List<ProfileResponse>> response = profileClient.getFreelancerProfiles();
            if (response == null || response.getData() == null) {
                return List.of();
            }
            return response.getData();
        } catch (FeignException ex) {
            throw new ProfileServiceUnavailableException("The Profile Service is temporarily unavailable");
        }
    }
}
