package com.levosoft.microservice.brain;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.main.lazy-initialization=true"
        }
)
class WhatsappBrainServiceApplicationTests {

    @Test
    void contextLoads() {
    }

}
