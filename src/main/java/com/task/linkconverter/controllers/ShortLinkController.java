package com.task.linkconverter.controllers;

import com.task.linkconverter.config.AppConfig;
import com.task.linkconverter.model.RetrieveResponse;
import com.task.linkconverter.model.ShortenRequest;
import com.task.linkconverter.model.ShortenResponse;
import com.task.linkconverter.service.ShortLinkService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.URISyntaxException;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ShortLinkController {
    private final ShortLinkService service;
    private final AppConfig appConfig;

    @PostMapping("/shorten")
    public ResponseEntity<ShortenResponse> shortenUrl(
            @Valid
            @RequestBody
            ShortenRequest request,
            HttpServletRequest httpServletRequest
    ) {
        log.info("POST /shorten for URL: {}", request.getOriginalUrl());
        String userIp = getClientIp(httpServletRequest);
        String shortCode = service.shortenUrl(request.getOriginalUrl(), userIp);
        String fullUrl = constructFullUrl(shortCode);
        return ResponseEntity.ok(new ShortenResponse(fullUrl));
    }

    @GetMapping("/retrieve")
    public ResponseEntity<RetrieveResponse> retrieveUrl(@RequestParam String shortUrl) {
        log.info("GET /retrieve/{}", shortUrl);
        String shortCode = extractShortCode(shortUrl);
        String originalUrl = service.getOriginalUrl(shortCode);
        return ResponseEntity.ok(new RetrieveResponse(originalUrl));
    }

    private String constructFullUrl(String shortCode) {
        return appConfig.getBaseUrl() + "/" + shortCode;
    }

    private String extractShortCode(String shortUrl) {
        try {
            URI uri = new URI(shortUrl);
            String path = uri.getPath();
            String[] segments = path.split("/");
            return segments[segments.length - 1];
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid short URL format");
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String[] headersToCheck = {"X-Forwarded-For", "Proxy-Client-IP", "WL-Proxy-Client-IP"};

        for (String header : headersToCheck) {
            String ip = request.getHeader(header);
            if (isValidIp(ip)) {
                return processIp(ip);
            }
        }

        return processIp(request.getRemoteAddr());
    }

    private boolean isValidIp(String ip) {
        return ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip);
    }

    private String processIp(String ip) {
        return ip.split(",")[0].trim();
    }
}
