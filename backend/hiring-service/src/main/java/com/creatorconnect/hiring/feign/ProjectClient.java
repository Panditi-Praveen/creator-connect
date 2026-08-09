package com.creatorconnect.hiring.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

/**
 * Declarative Feign client for the Project Service.
 *
 * <p>Resolved through Eureka by the registered service name
 * ({@code project-service}, port 8083) and load-balanced by Spring Cloud
 * LoadBalancer — the same discovery mechanism the API Gateway uses, so no URL
 * is hard-coded here. Only {@code GET /projects/{id}} is needed: it proves a
 * project exists and returns its owner ({@code userId}) for the creator
 * ownership checks performed by the application and review services.
 *
 * <p>The caller's {@code Authorization} header is forwarded by
 * {@code FeignClientConfig} so the Project Service can authenticate the call
 * with the same JWT the client presented to the Hiring Service.
 */
@FeignClient(name = "project-service")
public interface ProjectClient {

    /**
     * Fetches the project with the given id from the Project Service.
     *
     * @param id the project's id
     * @return the Project Service success envelope carrying the project
     * @throws feign.FeignException.NotFound when the project does not exist
     *         (the Project Service answers {@code 404})
     */
    @GetMapping("/projects/{id}")
    ProjectApiResponse<ProjectResponse> getProject(@PathVariable("id") UUID id);
}
