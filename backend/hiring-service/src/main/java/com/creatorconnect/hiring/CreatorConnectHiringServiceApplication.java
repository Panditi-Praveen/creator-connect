package com.creatorconnect.hiring;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * CreatorConnect Hiring Service — entry point.
 *
 * Responsibilities:
 *  - Let freelancers apply to projects (one application per project per user)
 *  - List the caller's own applications and a project's incoming applications
 *  - Let creators accept/reject applications and freelancers withdraw their own
 *  - Validate JWTs issued by the Auth Service on every protected request
 *
 * The service registers itself with the Eureka Service Registry on startup
 * ({@link EnableDiscoveryClient}) so the API Gateway can route /hiring/**
 * requests to it (port 8084).
 */
@SpringBootApplication
@EnableDiscoveryClient
public class CreatorConnectHiringServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CreatorConnectHiringServiceApplication.class, args);
    }
}
