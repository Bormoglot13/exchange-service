package com.zerohub.challenge.client.validator;

import com.zerohub.challenge.client.exception.ValidationGenericException;
import lombok.SneakyThrows;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Validator;

import java.util.Objects;

public interface BaseValidator<D> extends org.springframework.validation.Validator {

    // TODO implemented
    Validator validator = null;
    @SneakyThrows
    default void validate(D dto, final BindingResult bindingResult) {
        // spring validate
        if (Objects.nonNull(validator)) {
            validator.validate(dto, bindingResult);
        }
        if (bindingResult.hasErrors()) {
            throw new ValidationGenericException(bindingResult);
        }
    }
}
