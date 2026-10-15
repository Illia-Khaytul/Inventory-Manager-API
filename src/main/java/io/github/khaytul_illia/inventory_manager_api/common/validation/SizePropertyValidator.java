package io.github.khaytul_illia.inventory_manager_api.common.validation;

import jakarta.activation.UnsupportedDataTypeException;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.UnexpectedTypeException;
import lombok.Getter;
import org.hibernate.validator.constraintvalidation.HibernateConstraintValidatorContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Map;

@Component
@Getter
public class SizePropertyValidator implements ConstraintValidator<SizeProperty, Object> {

    private final ValidationProperties validationProperties;
    private int min;
    private int max;

    public SizePropertyValidator(ValidationProperties validationProperties) {
        this.validationProperties = validationProperties;
    }

    @Override
    public void initialize(SizeProperty constraintAnnotation) {
        ValidationProperties.PropertyKey propertyKey = constraintAnnotation.key();
        int minOverride = constraintAnnotation.minOverride();
        int maxOverride = constraintAnnotation.maxOverride();

        ValidationProperties.ValueLimits limits = validationProperties
            .getValueLimits(propertyKey)
            .orElseThrow(() -> new IllegalStateException(String.format(
                "No validation properties with key value '%s' for @SizeProperty constraint",
                propertyKey
            )));

        min = minOverride < 0 ? limits.min() : minOverride;
        max = maxOverride < 0 ? limits.max() : maxOverride;
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        if(value == null){
            return true;
        }

        int valueLength = getValueLength(value);
        if(valueLength == -1){
            throw new UnexpectedTypeException(String.format("Constraint annotation SizeProperty does not support values of type '%s'", value.getClass().getName()));
        }

        boolean isValid = valueLength >= min && valueLength <= max;
        if(!isValid){
            HibernateConstraintValidatorContext hibernateContext = context.unwrap(HibernateConstraintValidatorContext.class);
            hibernateContext.disableDefaultConstraintViolation();
            hibernateContext
                .addMessageParameter("min", min)
                .addMessageParameter("max", max)
                .buildConstraintViolationWithTemplate(hibernateContext.getDefaultConstraintMessageTemplate())
                .addConstraintViolation();
        }

        return isValid;
    }

    private int getValueLength(Object value){
        if(value.getClass().isArray()){
            return Array.getLength(value);
        }

        return switch (value){
            case CharSequence charSequence -> charSequence.length();
            case Collection<?> collection -> collection.size();
            case Map<?, ?> map -> map.size();
            default -> -1;
        };
    }

}
