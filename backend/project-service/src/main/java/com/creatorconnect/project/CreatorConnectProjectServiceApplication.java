package com.creatorconnect.project;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * CreatorConnect Project Service — entry point.
 *
 * Responsibilities:
 *  - Post creative projects (video editing, photography, design, writing, ...)
 *  - Browse all projects, view a single project, or list the caller's own
 *  - Update and delete projects (owner only)
 *  - Validate JWTs issued by the Auth Service on every protected request
 *  - Enrich project reads with the owner's public profile via the Profile
 *    Service (OpenFeign, resolved through Eureka)
 *
 * The service registers itself with the Eureka Service Registry on startup
 * ({@link EnableDiscoveryClient}) so the API Gateway can route /projects/**
 * requests to it (port 8083). Feign clients (the Profile Service integration)
 * are enabled via {@link EnableFeignClients}.
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
public class CreatorConnectProjectServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CreatorConnectProjectServiceApplication.class, args);
    }
}
