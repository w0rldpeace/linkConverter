package com.task.linkconverter;


import com.task.linkconverter.model.RetrieveResponse;
import com.task.linkconverter.model.ShortenRequest;
import com.task.linkconverter.model.ShortenResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ShortLinkControllerIT {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void shortenValidUrl_ReturnsShortUrl() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<ShortenRequest> request = new HttpEntity<>(
                new ShortenRequest("https://www.example.com/valid"),
                headers
        );

        ResponseEntity<ShortenResponse> response = restTemplate.postForEntity(
                "/api/shorten",
                request,
                ShortenResponse.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().getShortLink().startsWith("http://localhost:8080/"));
    }

    @Test
    void shortenInvalidUrl_ReturnsInternalServerError() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> request = new HttpEntity<>(
                "{\"originalUrl\":\"invalid-url\"}",
                headers
        );

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/shorten",
                request,
                String.class
        );

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }

    @Test
    void retrieveValidShortUrl_ReturnsOriginalUrl() {
        // First create a short link
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String originalUrl = "https://www.example.com/retrieve-test";

        ShortenResponse shortenResponse = restTemplate.postForObject(
                "/api/shorten",
                new HttpEntity<>(new ShortenRequest(originalUrl), headers),
                ShortenResponse.class
        );

        // Then retrieve it
        ResponseEntity<RetrieveResponse> response = restTemplate.getForEntity(
                "/api/retrieve?shortUrl=" + shortenResponse.getShortLink(),
                RetrieveResponse.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(originalUrl, response.getBody().getOriginalUrl());
    }
}