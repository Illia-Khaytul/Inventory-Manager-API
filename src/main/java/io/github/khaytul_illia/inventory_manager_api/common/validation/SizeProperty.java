package io.github.khaytul_illia.inventory_manager_api.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = SizePropertyValidator.class)
public @interface SizeProperty {

    String message() default "{jakarta.validation.constraints.Size.message}";

    Class<?>[] groups() default { };

    Class<? extends Payload>[] payload() default { };

    ValidationProperties.PropertyKey key();

    int minOverride() default -1;

    int maxOverride() default -1;

}
