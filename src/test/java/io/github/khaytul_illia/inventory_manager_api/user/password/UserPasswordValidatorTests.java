package io.github.khaytul_illia.inventory_manager_api.user.password;

import io.github.khaytul_illia.inventory_manager_api.error.exception.InvalidPasswordException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("UserPasswordValidator tests")
public class UserPasswordValidatorTests {

    private static final int minLength = 6;
    private static final int maxLength = 50;
    private static final int minDigitAmount = 2;
    private final UserPasswordValidator passwordValidator = new UserPasswordValidator(minLength, maxLength, minDigitAmount);

    @ParameterizedTest
    @MethodSource("provideInvalidPasswords")
    @DisplayName("Should throw InvalidPasswordException when password is null or invalid")
    void shouldThrowInvalidPasswordException_whenPasswordIsInvalid(String password, List<String> errorMessages){
        //Act and Assert
        assertThatThrownBy(() -> passwordValidator.validateUserPassword(password))
            .isInstanceOf(InvalidPasswordException.class)
            .satisfies(e -> {
                InvalidPasswordException exception = (InvalidPasswordException) e;

                assertThat(exception.getMessage()).isEqualTo("Invalid password");
                assertThat(exception.getErrorMessages()).containsAll(errorMessages);
            });
    }

    @Test
    @DisplayName("Should do nothing when password is valid")
    void should_when(){
        //Arrange
        String validPassword = "validPassword656";

        //Act
        passwordValidator.validateUserPassword(validPassword);
    }

    /*
            Test data provider methods
     */

    static Stream<Arguments> provideInvalidPasswords(){
        return Stream.of(
            //Null password
            Arguments.of(
                null,
                List.of("User password cannot be null or blank.")
            ),
            //Empty password
            Arguments.of(
                "",
                List.of("User password cannot be null or blank.")
            ),
            //Blank password
            Arguments.of(
                "          ",
                List.of("User password cannot be null or blank.")
            ),
            //Password too short and has no numbers
            Arguments.of(
                "a".repeat(minLength - 1),
                List.of(
                    String.format("Password must be %s or more characters in length.", minLength),
                    String.format("Password must contain %s or more digit characters.", minDigitAmount)
                )
            ),
            //Password too long and has spaces
            Arguments.of(
                "a".repeat(maxLength + 1) + " ",
                List.of(
                    String.format("Password must be no more than %s characters in length.", maxLength),
                    "Password contains a whitespace character."
                )
            ),
            //Invalid password
            Arguments.of(
                "some weird password",
                List.of(
                    String.format("Password must contain %s or more digit characters.", minDigitAmount),
                    "Password contains a whitespace character."
                )
            )
        );
    }

}
