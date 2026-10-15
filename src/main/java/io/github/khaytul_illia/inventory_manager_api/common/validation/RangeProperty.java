package io.github.khaytul_illia.inventory_manager_api.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import org.hibernate.validator.constraints.Range;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = RangePropertyValidator.class)
public @interface RangeProperty {

    String message() default "{org.hibernate.validator.constraints.Range.message}";

    Class<?>[] groups() default { };

    Class<? extends Payload>[] payload() default { };

    ValidationProperties.PropertyKey key();

    String minOverride() default "";

    String maxOverride() default "";

}
