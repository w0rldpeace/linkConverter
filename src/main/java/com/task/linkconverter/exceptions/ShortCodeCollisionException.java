package com.task.linkconverter.exceptions;

public class ShortCodeCollisionException extends RuntimeException {
    public ShortCodeCollisionException(String message) {
        super(message);
    }
}