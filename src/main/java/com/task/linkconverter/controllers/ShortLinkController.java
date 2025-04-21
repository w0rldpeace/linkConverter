package com.task.linkconverter.controllers;

import com.task.linkconverter.model.RetrieveResponse;
import com.task.linkconverter.model.ShortenRequest;
import com.task.linkconverter.model.ShortenResponse;
import com.task.linkconverter.service.ShortLinkService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ShortLinkController {
    private final ShortLinkService service;

    @PostMapping("/shorten")
    public ResponseEntity<ShortenResponse> shortenUrl(@Valid @RequestBody ShortenRequest request) {
        String shortLink = service.shortenUrl(request.getOriginalUrl());
        return ResponseEntity.ok(new ShortenResponse(shortLink));
    }

    @GetMapping("/retrieve/{shortLink}")
    public ResponseEntity<RetrieveResponse> retrieveUrl(@PathVariable String shortLink) {
        String originalUrl = service.getOriginalUrl(shortLink);
        return ResponseEntity.ok(new RetrieveResponse(originalUrl));
    }
}
