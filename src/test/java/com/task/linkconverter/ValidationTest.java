package com.task.linkconverter;

import com.task.linkconverter.model.ShortenRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void validUrl_NoViolations() {
        ShortenRequest request = new ShortenRequest("https://www.valid.com/path?query=param");
        Set<ConstraintViolation<ShortenRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty());
    }

    @Test
    void invalidUrl_ReturnsViolations() {
        ShortenRequest request = new ShortenRequest("ftp://invalid.protocol");
        Set<ConstraintViolation<ShortenRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
    }
}
