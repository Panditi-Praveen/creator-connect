package com.creatorconnect.ai.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

/**
 * Declarative Feign client for the Profile Service.
 *
 * <p>Resolved through Eureka by the registered service name
 * ({@code profile-service}, port 8082) and load-balanced by Spring Cloud
 * LoadBalancer — the same discovery mechanism the API Gateway uses, so no URL
 * is hard-coded here.
 *
 * <p>The caller's {@code Authorization} header is forwarded by
 * {@code FeignClientConfig} so the Profile Service can authenticate the call
 * with the same JWT the client presented to the AI Service.
 */
@FeignClient(name = "profile-service")
public interface ProfileClient {

    /**
     * Fetches the full talent pool (every profile in the unified
     * creator/freelancer model).
     *
     * @return the Profile Service success envelope carrying the profile list
     * @throws feign.FeignException when the Profile Service answers an error
     *         status or is unreachable
     */
    @GetMapping("/profile/freelancers")
    ProfileApiResponse<List<ProfileResponse>> getFreelancerProfiles();
}
