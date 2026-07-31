package com.creatorconnect.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * CreatorConnect Auth Service — entry point.
 *
 * Responsibilities:
 *  - User registration &amp; login
 *  - JWT token generation / validation
 *  - Role-based access control (ADMIN, CREATOR, FREELANCER)
 *
 * The service registers itself with the Eureka Service Registry on startup
 * ({@link EnableDiscoveryClient}) so the API Gateway can route /auth/** requests to it.
 */
@SpringBootApplication
@EnableDiscoveryClient
public class CreatorConnectAuthServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CreatorConnectAuthServiceApplication.class, args);
    }
}
