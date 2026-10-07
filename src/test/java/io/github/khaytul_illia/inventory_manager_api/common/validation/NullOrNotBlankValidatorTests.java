package io.github.khaytul_illia.inventory_manager_api.common.validation;

import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
@DisplayName("NullOrNotBlankValidator tests")
public class NullOrNotBlankValidatorTests {

    private final NullOrNotBlankValidator validator = new NullOrNotBlankValidator();

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    @DisplayName("Should return false when value is not null and blank")
    void shouldReturnFalse_whenValueIsInvalid(String value){
        //Arrange
        ConstraintValidatorContext context = mock(ConstraintValidatorContext.class);

        //Act
        boolean isValid = validator.isValid(value, context);

        //Assert
        assertThat(isValid).isFalse();
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"valid_value", "   valid_value_2   "})
    @DisplayName("Should return true when value is null or not blank")
    void shouldReturnFalse_whenValueIsValid(String value){
        //Arrange
        ConstraintValidatorContext context = mock(ConstraintValidatorContext.class);

        //Act
        boolean isValid = validator.isValid(value, context);

        //Assert
        assertThat(isValid).isTrue();
    }

}
