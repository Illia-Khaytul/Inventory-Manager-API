package io.github.khaytul_illia.inventory_manager_api.user;

import io.github.khaytul_illia.inventory_manager_api.error.exception.DuplicateEntryException;
import io.github.khaytul_illia.inventory_manager_api.user.password.UserPasswordValidator;
import io.github.khaytul_illia.inventory_manager_api.user.request.CreateUserRequest;
import io.github.khaytul_illia.inventory_manager_api.user.response.UserResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService tests")
public class UserServiceTests {

    @Mock
    private UserRepository userRepository;
    @Mock
    private UserPasswordValidator passwordValidator;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private UserMapper userMapper;
    @InjectMocks
    private UserService userService;

    @Nested
    @DisplayName("createUser tests")
    class CreateUserTests{

        private final String username = "username";
        private final String password = "password";
        private final CreateUserRequest request = new CreateUserRequest(username, password);
        private final User.UserRole role = User.UserRole.CUSTOMER;

        @Test
        @DisplayName("Should throw DuplicateEntryException when the provided username is already taken")
        void shouldThrowDuplicateEntryException_whenUsernameIsTaken(){
            //Arrange
            doNothing()
                .when(passwordValidator).validateUserPassword(password);
            when(userRepository.existsByUsername(username))
                .thenReturn(true);

            //Act and Assert
            assertThatThrownBy(() -> userService.createUser(request, role))
                .isInstanceOf(DuplicateEntryException.class)
                .hasMessage(String.format("Username '%s' is not unique", username));

            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("Should create new user when the provided username is unique")
        void shouldCreateNewUser_whenUsernameIsUnique(){
            //Arrange
            User user = new User(1L, username, password, role);
            UserResponse userResponse = new UserResponse(1L, username, role);

            doNothing()
                .when(passwordValidator).validateUserPassword(password);
            when(userRepository.existsByUsername(username))
                .thenReturn(false);
            when(passwordEncoder.encode(password))
                .thenReturn(password);
            when(userMapper.buildUser(username, password, role))
                .thenReturn(user);
            when(userRepository.save(user))
                .thenReturn(user);
            when(userMapper.toUserResponse(user))
                .thenReturn(userResponse);

            //Act
            UserResponse response = userService.createUser(request, role);

            //Assert
            assertThat(response).isNotNull();
            assertThat(response.id()).isEqualTo(userResponse.id());
            assertThat(response.username()).isEqualTo(userResponse.username());
            assertThat(response.role()).isEqualTo(userResponse.role());
        }

    }

}
