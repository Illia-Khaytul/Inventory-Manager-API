package io.github.khaytul_illia.inventory_manager_api.user;

import io.github.khaytul_illia.inventory_manager_api.error.exception.DuplicateEntryException;
import io.github.khaytul_illia.inventory_manager_api.error.exception.InvalidPasswordException;
import io.github.khaytul_illia.inventory_manager_api.security.SecurityUtils;
import io.github.khaytul_illia.inventory_manager_api.user.password.UserPasswordValidator;
import io.github.khaytul_illia.inventory_manager_api.user.request.CreateUserRequest;
import io.github.khaytul_illia.inventory_manager_api.user.request.PasswordChangeRequest;
import io.github.khaytul_illia.inventory_manager_api.user.response.UserResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;

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
    private SecurityUtils securityUtils;
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

    @Nested
    @DisplayName("changePassword tests")
    class ChangePasswordTests{

        private final String newPassword = "newPassword";
        private final String oldPassword = "oldPassword";
        private final PasswordChangeRequest request = new PasswordChangeRequest(oldPassword, newPassword);

        @Test
        @DisplayName("Should throw InvalidPasswordException when the new password is the same as the old password")
        void shouldThrowInvalidPasswordException_whenOldAndNewPasswordsMatch(){
            //Arrange
            PasswordChangeRequest request = new PasswordChangeRequest(oldPassword, oldPassword);

            //Act and Assert
            assertThatThrownBy(() -> userService.changePassword(request))
                .isInstanceOf(InvalidPasswordException.class)
                .satisfies(e -> {
                    InvalidPasswordException exception = (InvalidPasswordException) e;

                    assertThat(exception.getMessage()).isEqualTo("Invalid password");
                    assertThat(exception.getErrorMessages()).containsExactlyInAnyOrder("New password cannot be the same as old password.");
                });
        }

        @Test
        @DisplayName("Should throw InvalidPasswordException when the provided old password does not match the existing old password")
        void shouldThrowInvalidPasswordException_whenProvidedAndExistingOldPasswordsDoNotMatch(){
            //Arrange
            User authenticatedUser = new User(1L, "username", oldPassword + "_hash", User.UserRole.CUSTOMER);

            doNothing()
                .when(passwordValidator).validateUserPassword(newPassword);
            when(securityUtils.loadAuthenticatedUser())
                .thenReturn(authenticatedUser);
            when(passwordEncoder.matches(oldPassword, authenticatedUser.getPassword()))
                .thenReturn(false);

            //Act and Assert
            assertThatThrownBy(() -> userService.changePassword(request))
                .isInstanceOf(InvalidPasswordException.class)
                .satisfies(e -> {
                    InvalidPasswordException exception = (InvalidPasswordException) e;

                    assertThat(exception.getMessage()).isEqualTo("Invalid password change attempt");
                    assertThat(exception.getErrorMessages()).containsExactlyInAnyOrder("Provided old password must match existing old password.");
                });

            verify(passwordValidator).validateUserPassword(newPassword);
            verify(securityUtils).loadAuthenticatedUser();
            verify(passwordEncoder).matches(oldPassword, authenticatedUser.getPassword());
        }

        @Test
        @DisplayName("Should change user password when the new password is valid")
        void shouldChangeUserPassword_whenNewPasswordIsValid(){
            //Arrange
            User authenticatedUser = new User(1L, "username", oldPassword, User.UserRole.CUSTOMER);

            doNothing()
                .when(passwordValidator).validateUserPassword(newPassword);
            when(securityUtils.loadAuthenticatedUser())
                .thenReturn(authenticatedUser);
            when(passwordEncoder.matches(oldPassword, authenticatedUser.getPassword()))
                .thenReturn(true);
            when(passwordEncoder.encode(newPassword))
                .thenReturn(newPassword);

            //Act
            userService.changePassword(request);

            //Assert
            assertThat(authenticatedUser.getPassword()).isEqualTo(newPassword);

            verify(passwordValidator).validateUserPassword(newPassword);
            verify(securityUtils).loadAuthenticatedUser();
            verify(passwordEncoder).matches(oldPassword, oldPassword);
            verify(passwordEncoder).encode(newPassword);
        }

    }

    @Nested
    @DisplayName("deleteUser tests")
    class DeleteUserTests{

        @Test
        @DisplayName("Should delete the authenticated user account")
        void shouldDeleteUserAccount(){
            //Arrange
            String username = "username";
            Jwt jwt = mock(Jwt.class);

            when(securityUtils.getAuthenticatedUserAccessToken())
                .thenReturn(jwt);
            when(jwt.getSubject())
                .thenReturn(username);
            doNothing()
                .when(userRepository).deleteByUsername(username);

            //Act
            userService.deleteUser();

            //Assert
            verify(securityUtils).getAuthenticatedUserAccessToken();
            verify(userRepository).deleteByUsername(username);
        }

    }

}
