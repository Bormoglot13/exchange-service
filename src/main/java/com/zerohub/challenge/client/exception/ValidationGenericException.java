package com.zerohub.challenge.client.exception;

import org.springframework.validation.BindingResult;

public class ValidationGenericException extends RuntimeException {

    private final transient BindingResult bindingResult;

    public ValidationGenericException(BindingResult bindingResult) {
        this.bindingResult = bindingResult;
    }

    public BindingResult getBindingResult() {
        return bindingResult;
    }

}
