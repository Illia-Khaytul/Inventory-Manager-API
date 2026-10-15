package io.github.khaytul_illia.inventory_manager_api.common.validation;

import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.UnexpectedTypeException;
import org.hibernate.validator.constraintvalidation.HibernateConstraintValidatorContext;
import org.hibernate.validator.constraintvalidation.HibernateConstraintViolationBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotZeroValidator tests")
public class SizePropertyValidatorTests {

    private final ValidationProperties.PropertyKey propertyKey = ValidationProperties.PropertyKey.USER_USERNAME;
    private final int minValue = 2;
    private final int maxValue = 5;
    @Mock
    private ValidationProperties validationProperties;
    @InjectMocks
    private SizePropertyValidator sizePropertyValidator;

    @Nested
    @DisplayName("initialize tests")
    class InitializeTests{

        private SizeProperty sizeProperty;

        @BeforeEach
        void beforeEach(){
            sizeProperty = mock(SizeProperty.class);
        }

        @Test
        @DisplayName("Should throw IllegalStateException when no value limits properties exist for provided key")
        void shouldThrowIllegalStateException_whenValueLimitsDoNotExist(){
            //Arrange
            when(sizeProperty.key())
                .thenReturn(propertyKey);
            when(sizeProperty.minOverride())
                .thenReturn(-1);
            when(sizeProperty.maxOverride())
                .thenReturn(-1);
            when(validationProperties.getValueLimits(propertyKey))
                .thenReturn(Optional.empty());

            //Act and Assert
            assertThatThrownBy(() -> sizePropertyValidator.initialize(sizeProperty))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(String.format(
                    "No validation properties with key value '%s' for @SizeProperty constraint",
                    propertyKey
                ));

            verify(validationProperties).getValueLimits(propertyKey);
        }

        @Test
        @DisplayName("Should set min and max to override values when override values are not negative")
        void shouldSetOverrideValues_whenOverrideValuesNotNegative(){
            //Arrange
            int minOverride = minValue + 1;
            int maxOverride = maxValue + 1;
            ValidationProperties.ValueLimits limits = new ValidationProperties.ValueLimits(minValue, maxValue);

            when(sizeProperty.key())
                .thenReturn(propertyKey);
            when(sizeProperty.minOverride())
                .thenReturn(minOverride);
            when(sizeProperty.maxOverride())
                .thenReturn(maxOverride);
            when(validationProperties.getValueLimits(propertyKey))
                .thenReturn(Optional.of(limits));

            //Act
            sizePropertyValidator.initialize(sizeProperty);

            //Assert
            assertThat(sizePropertyValidator.getMin()).isEqualTo(minOverride);
            assertThat(sizePropertyValidator.getMax()).isEqualTo(maxOverride);

            verify(validationProperties).getValueLimits(propertyKey);
        }

        @Test
        @DisplayName("Should set min and max to value limits when override values are negative")
        void shouldSetValueLimits_whenOverrideValuesNegative(){
            //Arrange
            ValidationProperties.ValueLimits limits = new ValidationProperties.ValueLimits(minValue, maxValue);

            when(sizeProperty.key())
                .thenReturn(propertyKey);
            when(sizeProperty.minOverride())
                .thenReturn(-1);
            when(sizeProperty.maxOverride())
                .thenReturn(-1);
            when(validationProperties.getValueLimits(propertyKey))
                .thenReturn(Optional.of(limits));

            //Act
            sizePropertyValidator.initialize(sizeProperty);

            //Assert
            assertThat(sizePropertyValidator.getMin()).isEqualTo(minValue);
            assertThat(sizePropertyValidator.getMax()).isEqualTo(maxValue);

            verify(validationProperties).getValueLimits(propertyKey);
        }

    }

    @Nested
    @DisplayName("isValid tests")
    class IsValidTests{

        private ConstraintValidatorContext validatorContext;

        @BeforeEach
        void beforeEach(){
            validatorContext = mock(ConstraintValidatorContext.class);

            SizeProperty sizeProperty = mock(SizeProperty.class);

            when(sizeProperty.key())
                .thenReturn(propertyKey);
            when(sizeProperty.minOverride())
                .thenReturn(-1);
            when(sizeProperty.maxOverride())
                .thenReturn(-1);
            when(validationProperties.getValueLimits(propertyKey))
                .thenReturn(Optional.of(new ValidationProperties.ValueLimits(minValue, maxValue)));

            sizePropertyValidator.initialize(sizeProperty);
        }

        @ParameterizedTest
        @MethodSource("provideUnsupportedValues")
        @DisplayName("Should throw UnexpectedTypeException when value is of an unsupported type")
        void shouldThrowUnexpectedTypeException_whenValueIsUnsupported(Object value){
            //Act and Assert
            assertThatThrownBy(() -> sizePropertyValidator.isValid(value, validatorContext))
                .isInstanceOf(UnexpectedTypeException.class)
                .hasMessage(String.format("Constraint annotation SizeProperty does not support values of type '%s'", value.getClass().getName()));
        }

        @ParameterizedTest
        @MethodSource("provideInvalidSizeValues")
        @DisplayName("Should return false when value size is invalid")
        void shouldReturnFalse_whenValueSizeInvalid(Object value){
            //Arrange
            String message = "message";
            HibernateConstraintValidatorContext hibernateContext = mock(HibernateConstraintValidatorContext.class);
            HibernateConstraintViolationBuilder hibernateViolationBuilder = mock(HibernateConstraintViolationBuilder.class);

            when(validatorContext.unwrap(HibernateConstraintValidatorContext.class))
                .thenReturn(hibernateContext);
            doNothing()
                .when(hibernateContext).disableDefaultConstraintViolation();
            when(hibernateContext.addMessageParameter("min", minValue))
                .thenReturn(hibernateContext);
            when(hibernateContext.addMessageParameter("max", maxValue))
                .thenReturn(hibernateContext);
            when(hibernateContext.getDefaultConstraintMessageTemplate())
                .thenReturn(message);
            when(hibernateContext.buildConstraintViolationWithTemplate(message))
                .thenReturn(hibernateViolationBuilder);
            when(hibernateViolationBuilder.addConstraintViolation())
                .thenReturn(hibernateContext);

            //Act
            boolean isValid = sizePropertyValidator.isValid(value, validatorContext);

            //Assert
            assertThat(isValid).isFalse();

            verify(validatorContext).unwrap(HibernateConstraintValidatorContext.class);
            verify(hibernateContext).disableDefaultConstraintViolation();
            verify(hibernateContext).addMessageParameter("min", minValue);
            verify(hibernateContext).addMessageParameter("max", maxValue);
            verify(hibernateContext).getDefaultConstraintMessageTemplate();
            verify(hibernateContext).buildConstraintViolationWithTemplate(message);
            verify(hibernateViolationBuilder).addConstraintViolation();
        }

        @ParameterizedTest
        @NullSource
        @MethodSource("provideValidSizeValues")
        @DisplayName("Should return true when value size is valid")
        void shouldReturnTrue_whenValueSizeValid(Object value){
            //Act
            boolean isValid = sizePropertyValidator.isValid(value, validatorContext);

            //Assert
            assertThat(isValid).isTrue();

            verify(validatorContext, never()).unwrap(HibernateConstraintValidatorContext.class);
        }

        /*
                Test data provider methods
         */

        static Stream<Arguments> provideUnsupportedValues(){
            return Stream.of(
                //Generic object
                Arguments.of(new Object()),
                //Number
                Arguments.of(11)
            );
        }

        static Stream<Arguments> provideInvalidSizeValues(){
            return Stream.of(
                //Short string
                Arguments.of(""),
                //Long string
                Arguments.of("qwertyuiop"),
                //Short collection
                Arguments.of(List.of()),
                //Long collection
                Arguments.of(List.of("", "", "", "", "", "", "", "", "")),
                //Short map
                Arguments.of(Map.of()),
                //Long map
                Arguments.of(Map.of("a", "a", "b", "b", "c", "c", "d", "d", "e", "e", "f", "f", "h", "h")),
                //Short array
                Arguments.of((Object) new int[]{}),
                //Long array
                Arguments.of((Object) new int[]{1, 2, 3, 4, 5, 6, 7})
            );
        }

        static Stream<Arguments> provideValidSizeValues(){
            return Stream.of(
                //String
                Arguments.of("12345"),
                //Collection
                Arguments.of(List.of(1, 2, 3, 4, 5)),
                //Map
                Arguments.of(Map.of("a", "a", "b", "b", "c", "c", "d", "d", "e", "e")),
                //Array
                Arguments.of((Object) new int[]{1, 2, 3, 4, 5})
            );
        }

    }

}
