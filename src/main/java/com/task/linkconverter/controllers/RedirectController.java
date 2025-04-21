package com.task.linkconverter.controllers;

import com.task.linkconverter.service.ShortLinkService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
@RequiredArgsConstructor
public class RedirectController {
    private final ShortLinkService service;

    @GetMapping("/{shortLink}")
    public String redirect(@PathVariable String shortLink) {
        String originalUrl = service.getOriginalUrl(shortLink);
        return "redirect:" + originalUrl;
    }
}