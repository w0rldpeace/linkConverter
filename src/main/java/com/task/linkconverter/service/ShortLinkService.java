package com.task.linkconverter.service;

import com.task.linkconverter.exceptions.LinkExpiredException;
import com.task.linkconverter.exceptions.LinkNotFoundException;
import com.task.linkconverter.exceptions.RateLimitExceededException;
import com.task.linkconverter.exceptions.ShortLinkCollisionException;
import com.task.linkconverter.interfaces.ShortLinkRepository;
import com.task.linkconverter.model.ShortLink;
import io.seruco.encoding.base62.Base62;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShortLinkService {
    private static final String GLOBAL_RATE_LIMIT_KEY = "global_rate_limit";
    private static final int GLOBAL_RATE_LIMIT = 100;
    private static final int WINDOW_MINUTES = 1;

    private final ShortLinkRepository repository;
    private final RedisTemplate<String, Integer> redisTemplate;

    public String shortenUrl(String originalUrl) {
        checkGlobalRateLimit();
        try {
            log.debug("Shortening URL: {}", originalUrl);
            return repository.findByOriginalUrl(originalUrl)
                    .map(existing -> {
                        log.info("Returning existing short code for URL: {}", originalUrl);
                        return existing.getShortLink();
                    })
                    .orElseGet(() -> createAndSaveShortLink(originalUrl));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void checkGlobalRateLimit() {
        Long currentCount = redisTemplate.opsForValue().increment(GLOBAL_RATE_LIMIT_KEY, 1);

        if (currentCount != null && currentCount == 1) {
            redisTemplate.expire(GLOBAL_RATE_LIMIT_KEY, WINDOW_MINUTES, TimeUnit.MINUTES);
        }

        if (currentCount != null && currentCount > GLOBAL_RATE_LIMIT) {
            throw new RateLimitExceededException("Global rate limit exceeded");
        }
    }

    private String createAndSaveShortLink(String originalUrl) {
        String shortCode = generateShortLink(originalUrl);
        if (repository.findByShortLink(shortCode).isPresent()) {
            log.error("Hash collision detected for URL: {}", originalUrl);
            throw new ShortLinkCollisionException("Short code collision detected");
        }

        ShortLink shortLink = new ShortLink(null, originalUrl, shortCode, Instant.now());
        repository.save(shortLink);
        log.info("Created new short link: {} -> {}", shortCode, originalUrl);
        return shortCode;
    }

    public String getOriginalUrl(String shortCode) {
        log.debug("Retrieving original URL for: {}", shortCode);
        ShortLink shortLink = repository.findByShortLink(shortCode)
                .orElseThrow(() -> {
                    log.warn("Short link not found: {}", shortCode);
                    return new LinkNotFoundException("Short link not found");
                });

        if (isExpired(shortLink)) {
            log.warn("Attempt to access expired link: {}", shortCode);
            throw new LinkExpiredException("Short link has expired");
        }

        return shortLink.getOriginalUrl();
    }

    private boolean isExpired(ShortLink shortLink) {
        return shortLink.getCreatedAt().plus(10, ChronoUnit.MINUTES).isBefore(Instant.now());
    }

    public String generateShortLink(String originalUrl) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(originalUrl.getBytes(StandardCharsets.UTF_8));

            Base62 base62 = Base62.createInstance();
            byte[] encoded = base62.encode(Arrays.copyOf(hash, 6));
            return new String(encoded, StandardCharsets.UTF_8);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error generating short code", e);
        }
    }
}