package com.task.linkconverter.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;

@Value
public class RetrieveResponse {
    String originalUrl;

    @JsonCreator
    public RetrieveResponse(@JsonProperty("originalUrl") String originalUrl) {
        this.originalUrl = originalUrl;
    }
}