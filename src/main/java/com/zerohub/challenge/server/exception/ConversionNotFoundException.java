package com.zerohub.challenge.server.exception;

public class ConversionNotFoundException extends RuntimeException {

    public ConversionNotFoundException(String message, IllegalArgumentException ex) {
        super(message, ex);
    }

    public ConversionNotFoundException(String message) {
        super(message);
    }
}
