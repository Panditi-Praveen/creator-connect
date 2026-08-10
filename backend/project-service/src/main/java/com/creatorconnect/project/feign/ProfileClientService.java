package com.creatorconnect.project.feign;

import feign.FeignException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

/**
 * Small façade over {@link ProfileClient} that unwraps the Profile Service's
 * success envelope and treats lookup failures as "no profile to show".
 *
 * <p>Unlike the Hiring Service's {@code ProjectClientService} — whose calls
 * back the Project Service are security-critical (project existence +
 * ownership) and therefore fail fast — this façade is deliberately
 * <em>best-effort</em>: the owner profile is purely presentational enrichment
 * on project reads. A user may legitimately post a project before creating a
 * profile (the Profile Service answers {@code 404}), and a Profile Service
 * outage must never take down the project feed. In both cases the project is
 * still returned with {@code ownerProfile} omitted.
 *
 * <p>Failures are logged at {@code DEBUG} (the service's configured log level
 * is {@code DEBUG}) so the skipped enrichment is traceable without polluting
 * normal operation.
 */
@Service
public class ProfileClientService {

    private static final Logger log = LoggerFactory.getLogger(ProfileClientService.class);

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
     * Fetches the public profile of the given user, if one exists.
     *
     * <p>An empty result — user has no profile ({@code 404}), the Profile
     * Service answered an empty payload, or the Profile Service is
     * unreachable/failed ({@code 5xx}, connection error…) — is normal here
     * and never throws.
     *
     * @param userId the profile owner's id
     * @return the user's public profile, or {@link Optional#empty()} when the
     *         profile cannot be resolved
     */
    public Optional<ProfileResponse> getProfile(UUID userId) {
        try {
            ProfileApiResponse<ProfileResponse> response = profileClient.getProfile(userId);
            if (response == null || response.getData() == null) {
                log.debug("Profile Service returned no profile for user {}, skipping owner enrichment", userId);
                return Optional.empty();
            }
            return Optional.of(response.getData());
        } catch (FeignException.NotFound ex) {
            log.debug("No profile exists for user {}, skipping owner enrichment", userId);
            return Optional.empty();
        } catch (FeignException ex) {
            log.debug("Profile Service answered an error while enriching user {}'s projects: {}", userId, ex.getMessage());
            return Optional.empty();
        } catch (RuntimeException ex) {
            // A fully-down Profile Service (or one not registered with Eureka)
            // makes the LoadBalancer throw a plain RuntimeException (e.g.
            // IllegalStateException "No instances available") *before* a Feign
            // request is even attempted — not a FeignException. Enrichment is
            // best-effort by contract, so this must never fail a project read.
            log.debug("Profile Service unavailable while enriching user {}'s projects: {}", userId, ex.getMessage());
            return Optional.empty();
        }
    }
}
