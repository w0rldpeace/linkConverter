package com.task.linkconverter.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Value;

@Value
public class ShortenRequest {
    @NotBlank(message = "URL cannot be empty")
    @Pattern(
            regexp = "^https://www\\.[a-zA-Z0-9-]+\\.[a-zA-Z]{2,5}(?:/[a-zA-Z0-9-._~!$&'()*+,;=:@/?%]*)?$",
            message = "URL must be in format: https://www.[domain].[2-5 letter TLD]/[optional path]"
    )
    String originalUrl;

    @JsonCreator
    public ShortenRequest(@JsonProperty("originalUrl") String originalUrl) {
        this.originalUrl = originalUrl;
    }
}