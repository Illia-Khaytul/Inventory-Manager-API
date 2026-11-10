package io.github.khaytul_illia.inventory_manager_api.common.validation;

import io.github.khaytul_illia.inventory_manager_api.common.monetary.Decimals;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.UnexpectedTypeException;
import lombok.Getter;
import org.hibernate.validator.constraintvalidation.HibernateConstraintValidatorContext;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.BigInteger;

@Component
@Getter
public class RangePropertyValidator implements ConstraintValidator<RangeProperty, Object> {

    private final ValidationProperties validationProperties;
    private BigDecimal min;
    private BigDecimal max;

    public RangePropertyValidator(ValidationProperties validationProperties) {
        this.validationProperties = validationProperties;
    }

    @Override
    public void initialize(RangeProperty constraintAnnotation) {
        ValidationProperties.PropertyKey propertyKey = constraintAnnotation.key();
        String minOverride = constraintAnnotation.minOverride();
        String maxOverride = constraintAnnotation.maxOverride();

        ValidationProperties.NumericLimits limits = validationProperties
            .getNumericLimits(propertyKey)
            .orElseThrow(() -> new IllegalStateException(String.format(
                "No validation properties with key value '%s' for @RangeProperty constraint",
                propertyKey
            )));

        min = minOverride.isBlank() ? limits.min() : Decimals.of(minOverride);
        max = maxOverride.isBlank() ? limits.max() : Decimals.of(maxOverride);
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        if(value == null){
            return true;
        }

        BigDecimal numericValue = getNumericValue(value);
        if(numericValue == null){
            throw new UnexpectedTypeException(String.format("Constraint annotation RangeProperty does not support values of type '%s'", value.getClass().getName()));
        }

        boolean isValid = numericValue.compareTo(min) >= 0 && numericValue.compareTo(max) <= 0;
        if(!isValid){
            HibernateConstraintValidatorContext hibernateContext = context.unwrap(HibernateConstraintValidatorContext.class);
            hibernateContext.disableDefaultConstraintViolation();
            hibernateContext
                .addMessageParameter("min", min.toPlainString())
                .addMessageParameter("max", max.toPlainString())
                .buildConstraintViolationWithTemplate(hibernateContext.getDefaultConstraintMessageTemplate())
                .addConstraintViolation();
        }

        return isValid;
    }

    private BigDecimal getNumericValue(Object value) {
        return switch (value){
            case BigDecimal bigDecimal -> bigDecimal;
            case BigInteger bigInteger -> Decimals.of(new BigDecimal(bigInteger));
            case String string -> Decimals.of(string);
            case Double doubleValue -> Decimals.of(doubleValue);
            case Float floatValue -> Decimals.of(floatValue);
            case Number number -> Decimals.of(number.longValue());
            default -> null;
        };
    }
}
