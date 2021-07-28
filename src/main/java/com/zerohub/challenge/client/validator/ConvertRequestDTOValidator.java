package com.zerohub.challenge.client.validator;

import com.zerohub.challenge.client.dto.ConvertRequestDTO;
import io.micrometer.core.instrument.util.StringUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConstructorBinding;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.util.ReflectionUtils;
import org.springframework.validation.Errors;
import javax.validation.Validator;

import javax.validation.ConstraintViolation;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
@ConstructorBinding
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class ConvertRequestDTOValidator implements BaseValidator<ConvertRequestDTO> {


    // javax.validation.Validator
    private final Validator validator;


    @Override
    public boolean supports(@NonNull Class<?> aClass) {
        return ConvertRequestDTO.class.equals(aClass);
    }

    @Override
    public void validate(@NonNull Object obj, @NonNull Errors errors) {
        Set<ConstraintViolation<Object>> validates = validator.validate(obj);

        for (ConstraintViolation<Object> constraintViolation : validates) {
            var propertyPath = constraintViolation.getPropertyPath().toString();
            String message = constraintViolation.getMessage();
            errors.rejectValue(propertyPath, "", message);
        }

        // TODO add custom validation
        List<String> required = null;
        ConvertRequestDTO entity = (ConvertRequestDTO) obj;
        check(required, entity, errors);
    }

    private void check(List<String> required, ConvertRequestDTO entity, Errors errors) {
        Field f = null;
        for (String field : required) {
            try {
                f = getField(entity, field);
                ReflectionUtils.makeAccessible(f);
                if ((Objects.isNull(f.get(entity)) || StringUtils.isBlank(f.get(entity).toString()))
                ) {
                    errors.rejectValue(field, "");
                }
            }
            catch (IllegalAccessException | NoSuchFieldException ex) {
                errors.rejectValue(field, getMessageKey(f, ex.getClass().getSimpleName()));
            }
        }
    }

    private Field getField(ConvertRequestDTO entity, String field) throws NoSuchFieldException {
        Field f;
        try {
            f = entity.getClass().getDeclaredField(field);
        }
        catch (NoSuchFieldException ex) {
            f = entity.getClass().getField(field);
        }
        return f;
    }

    private String getMessageKey(Field f, String exceptionName) {
        return (exceptionName == null && ((Class) f.getGenericType()).isAssignableFrom(String.class)) ?
                "only.notblank" : "only.notnull";
    }

}