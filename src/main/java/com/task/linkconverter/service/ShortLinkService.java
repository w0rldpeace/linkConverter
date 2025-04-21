package com.task.linkconverter.service;

import com.task.linkconverter.exceptions.LinkExpiredException;
import com.task.linkconverter.exceptions.LinkNotFoundException;
import com.task.linkconverter.exceptions.ShortCodeCollisionException;
import com.task.linkconverter.interfaces.ShortLinkRepository;
import com.task.linkconverter.model.ShortLink;
import io.seruco.encoding.base62.Base62;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;

@Service
@RequiredArgsConstructor
public class ShortLinkService {
    private final ShortLinkRepository repository;

    public String shortenUrl(String originalUrl) {
        return repository.findByOriginalUrl(originalUrl)
                .map(ShortLink::getShortLink)
                .orElseGet(() -> createAndSaveShortLink(originalUrl));
    }

    private String createAndSaveShortLink(String originalUrl) {
        String shortCode = generateShortLink(originalUrl);
        if (repository.findByShortLink(shortCode).isPresent()) {
            throw new ShortCodeCollisionException("Short code collision detected");
        }
        ShortLink shortLink = new ShortLink(null, originalUrl, shortCode, Instant.now());
        repository.save(shortLink);
        return shortCode;
    }

    public String getOriginalUrl(String shortCode) {
        ShortLink shortLink = repository.findByShortLink(shortCode)
                .orElseThrow(() -> new LinkNotFoundException("Short link not found"));
        if (isExpired(shortLink)) {
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
