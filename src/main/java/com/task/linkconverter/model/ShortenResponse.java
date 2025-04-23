package com.task.linkconverter.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;

@Value
public class ShortenResponse {
    String shortLink;

    @JsonCreator
    public ShortenResponse(@JsonProperty("shortLink") String shortLink) {
        this.shortLink = shortLink;
    }
}