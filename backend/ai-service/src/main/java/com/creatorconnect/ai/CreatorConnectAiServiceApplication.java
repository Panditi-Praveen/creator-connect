package com.creatorconnect.ai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * CreatorConnect AI Service — entry point.
 *
 * <p>AI-assisted talent discovery: accepts a natural-language hiring query
 * ({@code POST /ai/discover}), fetches the platform's freelancer profiles
 * from the Profile Service via OpenFeign, and asks an external LLM API to
 * interpret the query and rank the matching profiles.
 *
 * <p>The service registers itself with the Eureka Service Registry on startup
 * ({@link EnableDiscoveryClient}) so the API Gateway can route {@code /ai/**}
 * requests to it (port 8085). OpenFeign clients are discovered in the
 * {@code com.creatorconnect.ai.feign} package.
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
public class CreatorConnectAiServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CreatorConnectAiServiceApplication.class, args);
    }
}
