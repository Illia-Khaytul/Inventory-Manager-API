package io.github.khaytul_illia.inventory_manager_api.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class NotZeroValidator implements ConstraintValidator<NotZero, Number> {

    @Override
    public boolean isValid(Number value, ConstraintValidatorContext context) {
        return value == null || !value.equals(0);
    }

}
