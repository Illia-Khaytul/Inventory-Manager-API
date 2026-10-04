package io.github.khaytul_illia.inventory_manager_api.user;

import io.github.khaytul_illia.inventory_manager_api.user.request.CreateUserRequest;
import io.github.khaytul_illia.inventory_manager_api.user.response.UserResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BaseOperatorSeeder tests")
public class BaseOperatorSeederTests {

    private final String baseUsername = "username";
    private final String basePassword = "password";
    @Mock
    private UserService userService;
    @Mock
    private UserRepository userRepository;
    private BaseOperatorSeeder baseOperatorSeeder;

    @BeforeEach
    void beforeEach(){
        baseOperatorSeeder = new BaseOperatorSeeder(
            baseUsername,
            basePassword,
            userService,
            userRepository
        );
    }

    @ParameterizedTest
    @CsvSource(
        nullValues = "NULL",
        quoteCharacter = '"',
        textBlock = """
        NULL, valid_pa55word
        "", valid_pa55word
        "   ", valid_pa55word
        base_username, NULL
        base_username, ""
        base_username, "   "
        """)
    @DisplayName("Should throw InvalidStateException when initialized with null or blank base OPERATOR user credentials")
    void shouldThrowIllegalStateException_whenNullOrBlankBaseOperatorCredentials(String baseUsername, String basePassword){
        //Act and Assert
        assertThatThrownBy(() -> new BaseOperatorSeeder(baseUsername, basePassword, userService, userRepository))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Base OPERATOR user credentials cannot be null nor blank");
    }

    @ParameterizedTest
    @EnumSource(
        value = User.UserRole.class,
        names = "OPERATOR",
        mode = EnumSource.Mode.EXCLUDE
    )
    @DisplayName("Should throw IllegalStateException when base user already exists but is not an OPERATOR")
    void shouldThrowIllegalStateException_whenBaseUserExistsButNotOperator(User.UserRole role){
        //Arrange
        User user = new User(1L, baseUsername, basePassword + "_hashed", role);
        when(userRepository.findByUsername(baseUsername))
            .thenReturn(Optional.of(user));

        //Act and Assert
        assertThatThrownBy(() -> baseOperatorSeeder.run())
            .isInstanceOf(IllegalStateException.class)
            .hasMessage(String.format("User '%s' with id %s exists but is not an OPERATOR", user.getUsername(), user.getId()));

        verify(userRepository).findByUsername(baseUsername);
        verify(userService, never()).createUser(any(CreateUserRequest.class), any(User.UserRole.class));
    }

    @Test
    @DisplayName("Should do nothing when base OPERATOR user exists")
    void shouldDoNothing_whenBaseOperatorUserExists(){
        //Arrange
        User user = new User(1L, baseUsername, basePassword + "_hashed", User.UserRole.OPERATOR);
        when(userRepository.findByUsername(baseUsername))
            .thenReturn(Optional.of(user));

        //Act
        baseOperatorSeeder.run();

        //Assert
        verify(userRepository).findByUsername(baseUsername);
        verify(userService, never()).createUser(any(CreateUserRequest.class), any(User.UserRole.class));
    }

    @Test
    @DisplayName("Should create new base OPERATOR user when it doesn't exist")
    void shouldCreateNewOperatorUser_whenItDoesNotExist(){
        //Arrange
        UserResponse response = new UserResponse(1L, baseUsername, User.UserRole.OPERATOR);

        when(userRepository.findByUsername(baseUsername))
            .thenReturn(Optional.empty());
        when(userService.createUser(any(CreateUserRequest.class), any(User.UserRole.class)))
            .thenReturn(response);

        //Act
        baseOperatorSeeder.run();

        //Assert
        verify(userRepository).findByUsername(baseUsername);
        verify(userService).createUser(any(CreateUserRequest.class), any(User.UserRole.class));
    }

}
