package io.github.khaytul_illia.inventory_manager_api.user;

import io.github.khaytul_illia.inventory_manager_api.support.context.ServiceITContext;
import io.github.khaytul_illia.inventory_manager_api.user.request.CreateUserRequest;
import io.github.khaytul_illia.inventory_manager_api.user.request.PasswordChangeRequest;
import io.github.khaytul_illia.inventory_manager_api.user.response.UserResponse;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.TransactionDefinition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@DisplayName("UserService integration tests")
public class UserServiceIT extends ServiceITContext {

    @Autowired
    private UserService userService;
    @Autowired
    private UserRepository userRepository;

    private final String username = "username";
    private final String password = "password55";
    private String encodedPassword;
    private User user;

    @BeforeEach
    void beforeEach(){
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        encodedPassword = passwordEncoder.encode(password);

        user = userRepository.save(new User(null, username, encodedPassword, User.UserRole.CUSTOMER));
    }

    @AfterEach
    void afterEach(){
        userRepository.deleteAll();
    }

    @Nested
    @DisplayName("createUser integration tests")
    class CreateUserIT {

        @ParameterizedTest
        @EnumSource(User.UserRole.class)
        @DisplayName("Should create new user with provided role when provided username is not taken")
        void shouldCreateNewUser_whenUsernameIsUnique(User.UserRole role){
            //Arrange
            CreateUserRequest request = new CreateUserRequest(user.getUsername() + "_different", password);

            //Act
            UserResponse response = userService.createUser(request, role);

            //Assert
            assertThat(response).isNotNull();
            assertThat(response.role()).isEqualTo(role);
            assertThat(userRepository.count()).isEqualTo(2);
            assertThat(userRepository.findByUsername(request.username()).orElseThrow().getRole()).isEqualTo(role);
        }

    }

    @Nested
    @DisplayName("changePassword integration tests")
    class ChangePasswordIT{

        private final PasswordChangeRequest request = new PasswordChangeRequest(password, "newPa55word");

        @BeforeEach
        void beforeEach(){
            SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken(username, password, user.getRole().name())
            );
        }

        @AfterEach
        void afterEach(){
            SecurityContextHolder.getContext().setAuthentication(null);
        }

        @Test
        @DisplayName("Should throw OptimisticLockingFailureException when authenticated user gets deleted mid-operation")
        void shouldThrowOptimisticLockingFailureException_whenAuthenticatedUserIsDeletedConcurrently(){
            //Arrange
            doAnswer(invocation -> {
                transactionTemplate.executeWithoutResult(status -> userRepository.deleteById(user.getId()));

                return invocation.callRealMethod();
            })
                .when(passwordEncoder).matches(password, user.getPassword());

            //Act and Assert
            assertThatThrownBy(() -> userService.changePassword(request))
                .isInstanceOf(OptimisticLockingFailureException.class);

            assertThat(userRepository.count()).isEqualTo(0);
        }

        @Test
        @DisplayName("Should change authenticated user password when new password is valid")
        void shouldChangeUserPassword_whenNewPasswordValid(){
            //Act
            userService.changePassword(request);

            //Assert
            assertThat(userRepository.findById(user.getId()).orElseThrow().getPassword()).isNotEqualTo(encodedPassword);
        }

    }

    @Nested
    @DisplayName("deleteUser integration tests")
    class DeleteUserIT {

        @AfterEach
        void afterEach(){
            SecurityContextHolder.getContext().setAuthentication(null);
        }

        @Test
        @DisplayName("Should delete authenticated user")
        void shouldDeleteAuthenticatedUser(){
            //Arrange
            SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken(username, password, user.getRole().name())
            );

            //Act
            userService.deleteUser();

            //Assert
            assertThat(userRepository.count()).isEqualTo(0);
        }

    }

}
