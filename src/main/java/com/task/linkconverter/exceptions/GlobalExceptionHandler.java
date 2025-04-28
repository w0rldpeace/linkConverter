package com.task.linkconverter.exceptions;

import com.task.linkconverter.model.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({LinkNotFoundException.class})
    public ResponseEntity<ErrorResponse> handleLinkNotFound(RuntimeException ex) {
        log.warn("Link not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(ex.getMessage(), "NOT_FOUND"));
    }

    @ExceptionHandler({LinkExpiredException.class})
    public ResponseEntity<ErrorResponse> handleLinkExpired(RuntimeException ex) {
        log.warn("Expired link access attempt: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.GONE)
                .body(new ErrorResponse(ex.getMessage(), "EXPIRED"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneralException(Exception ex) {
        log.error("Unexpected error occurred", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Internal server error", "SERVER_ERROR"));
    }

    @ExceptionHandler({RateLimitExceededException.class})
    public ResponseEntity<ErrorResponse> handleRateLimitExceeded(RuntimeException ex) {
        log.warn("Rate limit exceeded: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(new ErrorResponse(ex.getMessage(), "RATE_LIMIT_EXCEEDED"));
    }
}