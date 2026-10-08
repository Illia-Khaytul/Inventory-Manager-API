package io.github.khaytul_illia.inventory_manager_api.common.validation;


import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotZeroValidator tests")
public class NotZeroValidatorTests {

    private final NotZeroValidator validator = new NotZeroValidator();

    @Test
    @DisplayName("Should return false when the value is 0")
    void shouldReturnFalse_whenValueIsZero(){
        //Arrange
        int value = 0;
        ConstraintValidatorContext context = mock(ConstraintValidatorContext.class);

        //Act
        boolean isValid = validator.isValid(value, context);

        //Assert
        assertThat(isValid).isFalse();
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, -5, 1, 10})
    @DisplayName("Should return true when the value is not 0")
    void shouldReturnTrue_whenValueNotZero(int value){
        //Arrange
        ConstraintValidatorContext context = mock(ConstraintValidatorContext.class);

        //Act
        boolean isValid = validator.isValid(value, context);

        //Assert
        assertThat(isValid).isTrue();
    }

}
