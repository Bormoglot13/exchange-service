package com.zerohub.challenge.server.exception;

public class ConvertException extends RuntimeException {

    public ConvertException(String message, NumberFormatException ex) {
        super(message, ex);
    }
}
