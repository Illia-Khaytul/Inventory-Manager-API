package io.github.khaytul_illia.inventory_manager_api.user;

import io.github.khaytul_illia.inventory_manager_api.support.context.ServiceITContext;
import io.github.khaytul_illia.inventory_manager_api.support.data.TestDataConstants;
import io.github.khaytul_illia.inventory_manager_api.support.data.UserTestDataFactory;
import io.github.khaytul_illia.inventory_manager_api.support.utils.TestAuthenticationUtils;
import io.github.khaytul_illia.inventory_manager_api.user.request.CreateUserRequest;
import io.github.khaytul_illia.inventory_manager_api.user.request.PasswordChangeRequest;
import io.github.khaytul_illia.inventory_manager_api.user.response.UserResponse;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.TransactionDefinition;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@DisplayName("UserService integration tests")
public class UserServiceIT extends ServiceITContext {

    @Autowired
    private UserService userService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    @Qualifier("userRepository")
    private UserRepository realUserRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private String encodedPassword;
    private User user;

    @BeforeEach
    void beforeEach(){
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        encodedPassword = passwordEncoder.encode(TestDataConstants.USER_PASSWORD);

        user = userRepository.save(UserTestDataFactory.defaults().password(encodedPassword).build());
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
            CreateUserRequest request = UserTestDataFactory.uniqueCreateUserRequest();

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

        private final PasswordChangeRequest request = UserTestDataFactory.DEFAULT_PASSWORD_CHANGE_REQUEST;

        @BeforeEach
        void beforeEach(){
            TestAuthenticationUtils.setCustomerAuthentication();
        }

        @AfterEach
        void afterEach(){
            TestAuthenticationUtils.resetAuthentication();
        }

        @Test
        @DisplayName("Should throw OptimisticLockingFailureException when authenticated user gets deleted mid-operation")
        void shouldThrowOptimisticLockingFailureException_whenAuthenticatedUserIsDeletedConcurrently(){
            //Arrange
            doAnswer(invocation -> {
                Optional<User> foundUser = realUserRepository.findByUsername(user.getUsername());

                transactionTemplate.executeWithoutResult(status -> userRepository.deleteById(user.getId()));

                return foundUser;
            })
                .when(userRepository).findByUsername(user.getUsername());

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
            TestAuthenticationUtils.resetAuthentication();
        }

        @Test
        @DisplayName("Should delete authenticated user")
        void shouldDeleteAuthenticatedUser(){
            //Arrange
            TestAuthenticationUtils.setCustomerAuthentication();

            //Act
            userService.deleteUser();

            //Assert
            assertThat(userRepository.count()).isEqualTo(0);
        }

    }

}
