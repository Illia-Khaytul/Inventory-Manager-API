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

    private final UserPasswordValidator passwordValidator = new UserPasswordValidator();

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
                "abc",
                List.of("Password must be 6 or more characters in length.", "Password must contain 2 or more digit characters.")
            ),
            //Password too long and has spaces
            Arguments.of(
                "qwertyuiopasdfghjklzxcvbnm qwertyuiopasdfghjklzxcvbnm",
                List.of("Password must be no more than 50 characters in length.", "Password contains a whitespace character.")
            ),
            //Invalid password
            Arguments.of(
                "some weird password",
                List.of("Password must contain 2 or more digit characters.", "Password contains a whitespace character.")
            )
        );
    }

}
