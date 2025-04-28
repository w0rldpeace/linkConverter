package com.task.linkconverter;

import com.task.linkconverter.exceptions.*;
import com.task.linkconverter.interfaces.ShortLinkRepository;
import com.task.linkconverter.model.ShortLink;
import com.task.linkconverter.service.ShortLinkService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShortLinkServiceTest {

    @Mock
    private ShortLinkRepository repository;

    @InjectMocks
    private ShortLinkService service;

    private final String testUserIp = "127.0.0.1";

    @Test
    void shortenUrl_NewUrl_CreatesNewEntry() {
        when(repository.findByOriginalUrl(any())).thenReturn(Optional.empty());
        when(repository.save(any())).thenReturn(new ShortLink());

        String result = service.shortenUrl("https://www.example.com", testUserIp);
        assertNotNull(result);
        verify(repository).save(any());
    }

    @Test
    void shortenUrl_ExistingUrl_ReturnsExistingCode() {
        ShortLink existing = new ShortLink(1L, "https://www.example.com", "abc123", Instant.now());
        when(repository.findByOriginalUrl(any())).thenReturn(Optional.of(existing));

        String result = service.shortenUrl("https://www.example.com", testUserIp);
        assertEquals("abc123", result);
    }

    @Test
    void getOriginalUrl_ValidCode_ReturnsUrl() {
        ShortLink existing = new ShortLink(1L, "https://www.example.com", "abc123", Instant.now());
        when(repository.findByShortLink(any())).thenReturn(Optional.of(existing));

        String result = service.getOriginalUrl("abc123");
        assertEquals("https://www.example.com", result);
    }

    @Test
    void getOriginalUrl_ExpiredLink_ThrowsException() {
        ShortLink expired = new ShortLink(1L, "https://www.example.com", "abc123",
                Instant.now().minus(11, ChronoUnit.MINUTES));
        when(repository.findByShortLink(any())).thenReturn(Optional.of(expired));

        assertThrows(LinkExpiredException.class, () -> service.getOriginalUrl("abc123"));
    }

    @Test
    void generateShortLink_ConsistentInput_ProducesSameOutput() {
        String url = "https://www.example.com";
        String hash1 = service.generateShortLink(url);
        String hash2 = service.generateShortLink(url);
        assertEquals(hash1, hash2);
    }

    @Test
    void shortenUrl_ExceedsRateLimit_ThrowsException() throws InterruptedException {
        when(repository.findByOriginalUrl(any())).thenReturn(Optional.empty());

        CountDownLatch saveBlocker = new CountDownLatch(1);
        when(repository.save(any())).thenAnswer(invocation -> {
            saveBlocker.await();
            return new ShortLink();
        });

        ExecutorService executor = Executors.newFixedThreadPool(100);
        CountDownLatch threadsStarted = new CountDownLatch(100);

        /**
         * Start 100 threads
         */
        for (int i = 0; i < 100; i++) {
            executor.submit(() -> {
                service.shortenUrl("https://www.example.com", testUserIp);
                threadsStarted.countDown();
            });
        }

        threadsStarted.await(2, TimeUnit.SECONDS);

        /**
         * 101st request should fail
         */
        assertThrows(RateLimitExceededException.class, () -> {
            service.shortenUrl("https://www.example.com", testUserIp);
        });

        saveBlocker.countDown();

        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.SECONDS);
    }

    @Test
    void rateLimiter_Cleanup_RemovesExpiredEntries() {
        String userIp = "192.168.1.1";

        service.shortenUrl("https://www.example.com", userIp);

        ConcurrentHashMap<String, AtomicInteger> rateLimiter = service.getRateLimiterForTesting();

        rateLimiter.entrySet().removeIf(entry -> entry.getValue().get() == 0);

        assertFalse(rateLimiter.containsKey(userIp));
    }

    @Test
    void checkRateLimit_AfterRelease_DecrementsCounter() {
        String userIp = "192.168.1.1";
        service.shortenUrl("https://www.example.com", userIp);

        ConcurrentHashMap<String, AtomicInteger> rateLimiter = service.getRateLimiterForTesting();

        assertFalse(rateLimiter.containsKey(userIp));
    }
}
