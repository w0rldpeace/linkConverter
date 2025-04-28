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
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShortLinkService {
    private final ShortLinkRepository repository;

    private final ConcurrentHashMap<String, AtomicInteger> rateLimiter = new ConcurrentHashMap<>();
    private final ScheduledExecutorService cleanupScheduler = Executors.newSingleThreadScheduledExecutor();

    /**
     * Self-cleaning every 5 minutes
     */
    @PostConstruct
    public void init() {
        cleanupScheduler.scheduleAtFixedRate(() ->
                        rateLimiter.entrySet().removeIf(entry -> entry.getValue().get() == 0),
                5, 5, TimeUnit.MINUTES
        );
    }

    private void checkRateLimit(String userIp) {
        AtomicInteger counter = rateLimiter.computeIfAbsent(userIp, k -> new AtomicInteger(0));
        int current = counter.incrementAndGet();
        if (current > 100) {
            counter.decrementAndGet();
            throw new RateLimitExceededException("Rate limit exceeded");
        }
    }

    private void releaseRateLimit(String userIp) {
        rateLimiter.computeIfPresent(userIp, (k, v) -> {
            v.decrementAndGet();
            return v.get() > 0 ? v : null;
        });
    }

    public String shortenUrl(String originalUrl, String userIp) {
        checkRateLimit(userIp);
        try {
            log.debug("Shortening URL: {}", originalUrl);
            return repository.findByOriginalUrl(originalUrl)
                    .map(existing -> {
                        log.info("Returning existing short code for URL: {}", originalUrl);
                        return existing.getShortLink();
                    })
                    .orElseGet(() -> createAndSaveShortLink(originalUrl));
        } finally {
            releaseRateLimit(userIp);
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

    public ConcurrentHashMap<String, AtomicInteger> getRateLimiterForTesting() {
        return rateLimiter;
    }
}
