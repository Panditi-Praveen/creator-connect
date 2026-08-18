package com.creatorconnect.hiring.feign;

import com.creatorconnect.hiring.dto.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

/**
 * Declarative Feign client for the Auth Service user-lookup endpoint.
 *
 * <p>Used to resolve email addresses and display names for transactional
 * email notifications. The service name matches the Eureka registration
 * name of the Auth Service.
 */
@FeignClient(name = "auth-service", path = "/auth")
public interface AuthClient {

    /**
     * Returns minimal user info by userId.
     *
     * @param userId the user's UUID
     * @return the user info wrapped in the standard ApiResponse envelope
     */
    @GetMapping("/users/{userId}")
    ApiResponse<UserInfoResponse> getUserInfo(@PathVariable("userId") UUID userId);
}
