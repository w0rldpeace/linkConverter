package com.task.linkconverter;

import com.task.linkconverter.exceptions.RateLimitExceededException;
import com.task.linkconverter.service.ShortLinkService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ShortLinkServiceIntegrationTest {

    @Container
    static GenericContainer<?> redisContainer = new GenericContainer<>(
            DockerImageName.parse("redis:7.2-alpine"))
            .withExposedPorts(6379)
            .waitingFor(Wait.forListeningPort());

    @Autowired
    private ShortLinkService shortLinkService;

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redisContainer::getHost);
        registry.add("spring.data.redis.port", () -> redisContainer.getMappedPort(6379));
    }

    @BeforeEach
    void resetGlobalCounter() throws Exception {
        redisContainer.execInContainer("redis-cli", "DEL", "global_rate_limit");
    }

    @Test
    void testGlobalRateLimitAcrossMultipleUsers() {
        IntStream.range(0, 100).forEach(i ->
                assertDoesNotThrow(() ->
                        shortLinkService.shortenUrl(url(i))));

        assertThrows(RateLimitExceededException.class, () ->
                shortLinkService.shortenUrl(url(101)));
    }

    @Test
    void testLimitResetAfterWindow() throws Exception {
        IntStream.range(0, 100).forEach(i ->
                shortLinkService.shortenUrl(url(i)));

        redisContainer.execInContainer(
                "redis-cli",
                "EXPIRE",
                "global_rate_limit",
                "0"
        );

        assertDoesNotThrow(() -> shortLinkService.shortenUrl(url(101)));
    }

    private String url(int index) {
        return "https://example.com/" + index;
    }
}
