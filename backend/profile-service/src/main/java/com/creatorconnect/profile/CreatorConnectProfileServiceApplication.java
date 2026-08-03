package com.creatorconnect.profile;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * CreatorConnect Profile Service — entry point.
 *
 * Responsibilities:
 *  - Store creator/freelancer profile information
 *  - Create, read, update and delete profiles for authenticated users
 *  - Validate JWTs issued by the Auth Service on every protected request
 *
 * The service registers itself with the Eureka Service Registry on startup
 * ({@link EnableDiscoveryClient}) so the API Gateway can route /profile/**
 * requests to it (port 8082).
 */
@SpringBootApplication
@EnableDiscoveryClient
public class CreatorConnectProfileServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CreatorConnectProfileServiceApplication.class, args);
    }
}
