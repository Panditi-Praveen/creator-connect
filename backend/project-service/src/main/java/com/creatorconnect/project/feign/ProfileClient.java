package com.creatorconnect.project.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

/**
 * Declarative Feign client for the Profile Service.
 *
 * <p>Resolved through Eureka by the registered service name
 * ({@code profile-service}, port 8082) and load-balanced by Spring Cloud
 * LoadBalancer — the same discovery mechanism the API Gateway uses, so no URL
 * is hard-coded here. Only {@code GET /profile/{userId}} is needed: it returns
 * the public profile of the user who owns a project, which the Project Service
 * attaches to project reads as {@code ownerProfile}.
 *
 * <p>The caller's {@code Authorization} header is forwarded by
 * {@code FeignClientConfig} so the Profile Service can authenticate the call
 * with the same JWT the client presented to the Project Service.
 */
@FeignClient(name = "profile-service")
public interface ProfileClient {

    /**
     * Fetches the public profile of the user with the given id.
     *
     * @param userId the profile owner's id
     * @return the Profile Service success envelope carrying the profile
     * @throws feign.FeignException.NotFound when the user has no profile
     *         (the Profile Service answers {@code 404})
     */
    @GetMapping("/profile/{userId}")
    ProfileApiResponse<ProfileResponse> getProfile(@PathVariable("userId") UUID userId);
}
