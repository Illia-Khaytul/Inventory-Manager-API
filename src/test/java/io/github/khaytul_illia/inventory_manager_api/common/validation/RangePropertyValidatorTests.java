package io.github.khaytul_illia.inventory_manager_api.common.validation;

import io.github.khaytul_illia.inventory_manager_api.common.monetary.Decimals;
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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RangePropertyValidator tests")
public class RangePropertyValidatorTests {

    private final ValidationProperties.PropertyKey propertyKey = ValidationProperties.PropertyKey.USER_USERNAME;
    private final BigDecimal minValue = Decimals.of(2);
    private final BigDecimal maxValue = Decimals.of(5);
    @Mock
    private ValidationProperties validationProperties;
    @InjectMocks
    private RangePropertyValidator rangePropertyValidator;

    @Nested
    @DisplayName("initialize tests")
    class InitializeTests{

        private RangeProperty rangeProperty;

        @BeforeEach
        void beforeEach(){
            rangeProperty = mock(RangeProperty.class);
        }

        @Test
        @DisplayName("Should throw IllegalStateException when no value limits properties exist for provided key")
        void shouldThrowIllegalStateException_whenValueLimitsDoNotExist(){
            //Arrange
            when(rangeProperty.key())
                .thenReturn(propertyKey);
            when(rangeProperty.minOverride())
                .thenReturn("");
            when(rangeProperty.maxOverride())
                .thenReturn("");
            when(validationProperties.getNumericLimits(propertyKey))
                .thenReturn(Optional.empty());

            //Act and Assert
            assertThatThrownBy(() -> rangePropertyValidator.initialize(rangeProperty))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(String.format(
                    "No validation properties with key value '%s' for @RangeProperty constraint",
                    propertyKey
                ));

            verify(validationProperties).getNumericLimits(propertyKey);
        }

        @Test
        @DisplayName("Should set min and max to override values when override values are not blank")
        void shouldSetOverrideValues_whenOverrideValuesNotBlank(){
            //Arrange
            String minOverride = minValue.add(BigDecimal.ONE).toPlainString();
            String maxOverride = maxValue.add(BigDecimal.ONE).toPlainString();
            ValidationProperties.NumericLimits limits = new ValidationProperties.NumericLimits(minValue, maxValue);

            when(rangeProperty.key())
                .thenReturn(propertyKey);
            when(rangeProperty.minOverride())
                .thenReturn(minOverride);
            when(rangeProperty.maxOverride())
                .thenReturn(maxOverride);
            when(validationProperties.getNumericLimits(propertyKey))
                .thenReturn(Optional.of(limits));

            //Act
            rangePropertyValidator.initialize(rangeProperty);

            //Assert
            assertThat(rangePropertyValidator.getMin()).isEqualTo(minOverride);
            assertThat(rangePropertyValidator.getMax()).isEqualTo(maxOverride);

            verify(validationProperties).getNumericLimits(propertyKey);
        }

        @Test
        @DisplayName("Should set min and max to value limits when override values are blank")
        void shouldSetValueLimits_whenOverrideValuesNegative(){
            //Arrange
            ValidationProperties.NumericLimits limits = new ValidationProperties.NumericLimits(minValue, maxValue);

            when(rangeProperty.key())
                .thenReturn(propertyKey);
            when(rangeProperty.minOverride())
                .thenReturn("");
            when(rangeProperty.maxOverride())
                .thenReturn("");
            when(validationProperties.getNumericLimits(propertyKey))
                .thenReturn(Optional.of(limits));

            //Act
            rangePropertyValidator.initialize(rangeProperty);

            //Assert
            assertThat(rangePropertyValidator.getMin()).isEqualTo(minValue);
            assertThat(rangePropertyValidator.getMax()).isEqualTo(maxValue);

            verify(validationProperties).getNumericLimits(propertyKey);
        }

    }

    @Nested
    @DisplayName("isValid tests")
    class IsValidTests{

        private ConstraintValidatorContext validatorContext;

        @BeforeEach
        void beforeEach(){
            validatorContext = mock(ConstraintValidatorContext.class);

            RangeProperty sizeProperty = mock(RangeProperty.class);

            when(sizeProperty.key())
                .thenReturn(propertyKey);
            when(sizeProperty.minOverride())
                .thenReturn("");
            when(sizeProperty.maxOverride())
                .thenReturn("");
            when(validationProperties.getNumericLimits(propertyKey))
                .thenReturn(Optional.of(new ValidationProperties.NumericLimits(minValue, maxValue)));

            rangePropertyValidator.initialize(sizeProperty);
        }

        @ParameterizedTest
        @MethodSource("provideUnsupportedValues")
        @DisplayName("Should throw UnexpectedTypeException when value is of an unsupported type")
        void shouldThrowUnexpectedTypeException_whenValueIsUnsupported(Object value){
            //Act and Assert
            assertThatThrownBy(() -> rangePropertyValidator.isValid(value, validatorContext))
                .isInstanceOf(UnexpectedTypeException.class)
                .hasMessage(String.format("Constraint annotation RangeProperty does not support values of type '%s'", value.getClass().getName()));
        }

        @ParameterizedTest
        @MethodSource("provideInvalidRangeValues")
        @DisplayName("Should return false when value range is invalid")
        void shouldReturnFalse_whenValueRangeInvalid(Object value){
            //Arrange
            String message = "message";
            HibernateConstraintValidatorContext hibernateContext = mock(HibernateConstraintValidatorContext.class);
            HibernateConstraintViolationBuilder hibernateViolationBuilder = mock(HibernateConstraintViolationBuilder.class);

            when(validatorContext.unwrap(HibernateConstraintValidatorContext.class))
                .thenReturn(hibernateContext);
            doNothing()
                .when(hibernateContext).disableDefaultConstraintViolation();
            when(hibernateContext.addMessageParameter("min", minValue.toPlainString()))
                .thenReturn(hibernateContext);
            when(hibernateContext.addMessageParameter("max", maxValue.toPlainString()))
                .thenReturn(hibernateContext);
            when(hibernateContext.getDefaultConstraintMessageTemplate())
                .thenReturn(message);
            when(hibernateContext.buildConstraintViolationWithTemplate(message))
                .thenReturn(hibernateViolationBuilder);
            when(hibernateViolationBuilder.addConstraintViolation())
                .thenReturn(hibernateContext);

            //Act
            boolean isValid = rangePropertyValidator.isValid(value, validatorContext);

            //Assert
            assertThat(isValid).isFalse();

            verify(validatorContext).unwrap(HibernateConstraintValidatorContext.class);
            verify(hibernateContext).disableDefaultConstraintViolation();
            verify(hibernateContext).addMessageParameter("min", minValue.toPlainString());
            verify(hibernateContext).addMessageParameter("max", maxValue.toPlainString());
            verify(hibernateContext).getDefaultConstraintMessageTemplate();
            verify(hibernateContext).buildConstraintViolationWithTemplate(message);
            verify(hibernateViolationBuilder).addConstraintViolation();
        }

        @ParameterizedTest
        @NullSource
        @MethodSource("provideValidRangeValues")
        @DisplayName("Should return true when value range is valid")
        void shouldReturnTrue_whenValueRangeValid(Object value){
            //Act
            boolean isValid = rangePropertyValidator.isValid(value, validatorContext);

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
                //Collection
                Arguments.of(List.of())
            );
        }

        static Stream<Arguments> provideInvalidRangeValues(){
            return Stream.of(
                //Short decimal
                Arguments.of(new BigDecimal("1.99")),
                //Long decimal
                Arguments.of(new BigDecimal("5.01")),
                //Short integer
                Arguments.of(BigInteger.ONE),
                //Long integer
                Arguments.of(BigInteger.TEN),
                //Short String number
                Arguments.of("1.99"),
                //Long String number
                Arguments.of("5.01"),
                //Short double
                Arguments.of(1.99d),
                //Long double
                Arguments.of(5.01d),
                //Short float
                Arguments.of(1.99f),
                //Long float
                Arguments.of(5.01f),
                //Short number
                Arguments.of(1),
                //Long number
                Arguments.of(6)
            );
        }

        static Stream<Arguments> provideValidRangeValues(){
            return Stream.of(
                //BigDecimal
                Arguments.of(new BigDecimal("2.01")),
                //BigInteger
                Arguments.of(BigInteger.TWO),
                //String number
                Arguments.of("5"),
                //double
                Arguments.of(4.99d),
                //float
                Arguments.of(2.01f),
                //number
                Arguments.of(3)
            );
        }

    }

}
