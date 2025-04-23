package com.task.linkconverter;

import com.task.linkconverter.controllers.RedirectController;
import com.task.linkconverter.exceptions.LinkExpiredException;
import com.task.linkconverter.service.ShortLinkService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedirectControllerTest {

    @Mock
    private ShortLinkService service;

    @InjectMocks
    private RedirectController controller;

    @Test
    void validRedirect_ReturnsRedirectView() {
        when(service.getOriginalUrl("valid")).thenReturn("https://www.example.com");
        String result = controller.redirect("valid");
        assertEquals("redirect:https://www.example.com", result);
    }

    @Test
    void expiredLink_ThrowsException() {
        when(service.getOriginalUrl("expired")).thenThrow(new LinkExpiredException("Expired"));
        assertThrows(LinkExpiredException.class, () -> controller.redirect("expired"));
    }
}
