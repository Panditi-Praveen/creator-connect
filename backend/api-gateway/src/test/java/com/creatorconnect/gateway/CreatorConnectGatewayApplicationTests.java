package com.creatorconnect.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
    "eureka.client.enabled=false",
    "spring.cloud.gateway.server.webmvc.enabled=false",
    "server.port=0"
})
class CreatorConnectGatewayApplicationTests {

    @Test
    void contextLoads() {
    }
}
