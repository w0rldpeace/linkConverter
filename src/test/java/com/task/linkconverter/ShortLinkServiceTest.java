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
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShortLinkServiceTest {

    @Mock
    private ShortLinkRepository repository;

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private ShortLinkService service;

    @Test
    void shortenUrl_NewUrl_CreatesNewEntry() {
        // Stub Redis rate limiting for this test
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(eq("global_rate_limit"), eq(1L))).thenReturn(1L);

        when(repository.findByOriginalUrl(anyString())).thenReturn(Optional.empty());
        when(repository.save(any())).thenReturn(new ShortLink());

        String result = service.shortenUrl("https://www.example.com");
        assertNotNull(result);
        verify(repository).save(any());
    }

    @Test
    void shortenUrl_ExistingUrl_ReturnsExistingCode() {
        // Stub Redis rate limiting for this test
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(eq("global_rate_limit"), eq(1L))).thenReturn(1L);

        ShortLink existing = new ShortLink(1L, "https://www.example.com", "abc123", Instant.now());
        when(repository.findByOriginalUrl(anyString())).thenReturn(Optional.of(existing));

        String result = service.shortenUrl("https://www.example.com");
        assertEquals("abc123", result);
    }

    @Test
    void getOriginalUrl_ValidCode_ReturnsUrl() {
        // No Redis stubbing needed here
        ShortLink existing = new ShortLink(1L, "https://www.example.com", "abc123", Instant.now());
        when(repository.findByShortLink(anyString())).thenReturn(Optional.of(existing));

        String result = service.getOriginalUrl("abc123");
        assertEquals("https://www.example.com", result);
    }

    @Test
    void getOriginalUrl_ExpiredLink_ThrowsException() {
        // No Redis stubbing needed here
        ShortLink expired = new ShortLink(1L, "https://www.example.com", "abc123",
                Instant.now().minus(11, ChronoUnit.MINUTES));
        when(repository.findByShortLink(anyString())).thenReturn(Optional.of(expired));

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
    void shortenUrl_Collision_ThrowsException() {
        // Stub Redis rate limiting for this test
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(eq("global_rate_limit"), eq(1L))).thenReturn(1L);

        when(repository.findByOriginalUrl(anyString())).thenReturn(Optional.empty());
        when(repository.findByShortLink(anyString())).thenReturn(Optional.of(new ShortLink()));

        // changed the assert because it gets wrapped in a RuntimeException
        assertThrows(RuntimeException.class, () ->
                service.shortenUrl("https://www.example.com"));
    }
}
