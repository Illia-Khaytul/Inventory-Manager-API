package io.github.khaytul_illia.inventory_manager_api.user;

import io.github.khaytul_illia.inventory_manager_api.user.response.UserResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mapstruct.factory.Mappers;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("UserMapper tests")
public class UserMapperTests {

    private final UserMapper userMapper = Mappers.getMapper(UserMapper.class);

    @ParameterizedTest
    @CsvSource(textBlock = """
        username1, password1, CUSTOMER
        another_user, differentPassword, OPERATOR
        """)
    @DisplayName("Should create new user instance with provided parameters")
    void shouldCreateNewUser(String username, String password, User.UserRole role){
        //Act
        User user = userMapper.buildUser(username, password, role);

        //Assert
        assertThat(user).isNotNull();
        assertThat(user.getId()).isNull();
        assertThat(user.getUsername()).isEqualTo(username);
        assertThat(user.getPassword()).isEqualTo(password);
        assertThat(user.getRole()).isEqualTo(role);
    }

    @ParameterizedTest
    @MethodSource("provideUserInstances")
    @DisplayName("Should map user instance to user response DTO")
    void shouldMapUserToUserResponse(User user){
        //Act
        UserResponse response = userMapper.toUserResponse(user);

        //Assert
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(user.getId());
        assertThat(response.username()).isEqualTo(user.getUsername());
        assertThat(response.role()).isEqualTo(user.getRole());
    }

    /*
            Test data provider methods
     */

    static Stream<Arguments> provideUserInstances(){
        return Stream.of(
            Arguments.of(
                new User(1L, "username", "password", User.UserRole.CUSTOMER)
            ),
            Arguments.of(
                new User(2L, "other_user", "password", User.UserRole.OPERATOR)
            )
        );
    }

}
