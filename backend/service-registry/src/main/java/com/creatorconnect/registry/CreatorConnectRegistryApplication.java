package com.creatorconnect.registry;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

@SpringBootApplication
@EnableEurekaServer
public class CreatorConnectRegistryApplication {

    public static void main(String[] args) {
        SpringApplication.run(CreatorConnectRegistryApplication.class, args);
    }
}
