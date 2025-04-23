package com.task.linkconverter.controllers;

import com.task.linkconverter.config.AppConfig;
import com.task.linkconverter.service.ShortLinkService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
public class RedirectController {
    private final ShortLinkService service;
    private final AppConfig appConfig;

    @GetMapping("/{shortLink}")
    public String redirect(@PathVariable String shortLink) {
        log.info("Redirect attempt for: {}", shortLink);
        String originalUrl = service.getOriginalUrl(shortLink);
        log.debug("Redirecting {} -> {}", shortLink, originalUrl);
        return "redirect:" + originalUrl;
    }
}
