package io.github.khaytul_illia.inventory_manager_api.user;

import io.github.khaytul_illia.inventory_manager_api.support.context.ControllerTestContext;
import io.github.khaytul_illia.inventory_manager_api.support.data.UserTestDataFactory;
import io.github.khaytul_illia.inventory_manager_api.user.request.CreateUserRequest;
import io.github.khaytul_illia.inventory_manager_api.user.request.PasswordChangeRequest;
import io.github.khaytul_illia.inventory_manager_api.user.response.UserResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Map;

import static io.github.khaytul_illia.inventory_manager_api.support.utils.mvc.ErrorResponseAssertionUtils.*;
import static io.github.khaytul_illia.inventory_manager_api.support.utils.mvc.SuccessResponseAssertionUtils.assertNoContentResponse;
import static io.github.khaytul_illia.inventory_manager_api.support.utils.mvc.SuccessResponseAssertionUtils.assertUserResponse;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("UserController tests")
public class UserControllerTests extends ControllerTestContext {

    @Nested
    @DisplayName("createCustomer endpoint tests")
    class CreateCustomerTests{

        @Test
        @WithMockUser
        @DisplayName("Should return 201 Created when successfully created new user with CUSTOMER role")
        void shouldReturn201_whenSuccessfullyCreatedCustomer() throws Exception{
            //Arrange
            CreateUserRequest request = UserTestDataFactory.DEFAULT_CREATE_REQUEST;
            UserResponse response = UserTestDataFactory.DEFAULT_CUSTOMER_RESPONSE;

            when(userService.createUser(request, User.UserRole.CUSTOMER))
                .thenReturn(response);

            //Act and Assert
            ResultActions result = mockMvc.perform(
                    post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isCreated());

            assertUserResponse(result, response);

            verify(userService).createUser(request, User.UserRole.CUSTOMER);
        }

        @Test
        @DisplayName("Should return 201 Created when accessed with no authentication")
        void shouldReturn201_whenNoAuthentication() throws Exception {
            //Arrange
            CreateUserRequest request = UserTestDataFactory.DEFAULT_CREATE_REQUEST;
            UserResponse response = UserTestDataFactory.DEFAULT_CUSTOMER_RESPONSE;

            when(userService.createUser(request, User.UserRole.CUSTOMER))
                .thenReturn(response);

            //Act and Assert
            mockMvc.perform(
                    post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isCreated());

            verify(userService).createUser(request, User.UserRole.CUSTOMER);
        }

        @ParameterizedTest
        @CsvSource(
            nullValues = "NULL",
            quoteCharacter = '"',
            textBlock = """
            NULL, NULL, must not be blank, must not be blank
            "", "", must not be blank, must not be blank
            "   ", "   ", must not be blank, must not be blank
            "qwertyuiopasdfghjklzxcvbnmqwertyuiopasdfghjklzxcvbnm", "qwertyuiopasdfghjklzxcvbnmqwertyuiopasdfghjklzxcvbnm", size must be between 0 and 50, size must be between 0 and 50
            """)
        @WithMockUser
        @DisplayName("Should return 400 Bad Request when invalid create user request fields")
        void shouldReturn400_whenInvalidCreateUserRequest(
            String username,
            String password,
            String usernameMessage,
            String passwordMessage
        ) throws Exception{
            //Arrange
            CreateUserRequest request = new CreateUserRequest(username, password);

            //Act and Assert
            ResultActions result = mockMvc.perform(
                    post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isBadRequest());

            assertSingleErrorValidationErrorResponse(result, Map.of("username", usernameMessage, "password", passwordMessage));
        }

    }

    @Nested
    @DisplayName("createOperator endpoint tests")
    class CreateOperatorTests{

        @Test
        @WithMockUser(roles = "OPERATOR")
        @DisplayName("Should return 201 Created when successfully created new user with OPERATOR role")
        void shouldReturn201_whenSuccessfullyCreatedOperator() throws Exception{
            //Arrange
            CreateUserRequest request = UserTestDataFactory.DEFAULT_CREATE_REQUEST;
            UserResponse response = UserTestDataFactory.defaults().id(1L).role(User.UserRole.OPERATOR).toResponse();

            when(userService.createUser(request, User.UserRole.OPERATOR))
                .thenReturn(response);

            //Act and Assert
            ResultActions result = mockMvc.perform(
                    post("/users/operators")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isCreated());

            assertUserResponse(result, response);

            verify(userService).createUser(request, User.UserRole.OPERATOR);
        }

        @Test
        @WithMockUser(roles = "CUSTOMER")
        @DisplayName("Should return 403 Forbidden when accessed with authentication but wrong´role")
        void shouldReturn403_whenAuthenticatedWithWrongRole() throws Exception {
            //Arrange
            CreateUserRequest request = UserTestDataFactory.DEFAULT_CREATE_REQUEST;

            //Act and Assert
            ResultActions result = mockMvc.perform(
                    post("/users/operators")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isForbidden());

            assertForbiddenErrorResponse(result);
        }

        @Test
        @DisplayName("Should return 401 Unauthorized when accessed with no authentication")
        void shouldReturn401_whenNoAuthentication() throws Exception {
            //Arrange
            CreateUserRequest request = UserTestDataFactory.DEFAULT_CREATE_REQUEST;

            //Act and Assert
            ResultActions result = mockMvc.perform(
                    post("/users/operators")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isUnauthorized());

            assertNoAuthErrorResponse(result);
        }

        @ParameterizedTest
        @CsvSource(
            nullValues = "NULL",
            quoteCharacter = '"',
            textBlock = """
            NULL, NULL, must not be blank, must not be blank
            "", "", must not be blank, must not be blank
            "   ", "   ", must not be blank, must not be blank
            "qwertyuiopasdfghjklzxcvbnmqwertyuiopasdfghjklzxcvbnm", "qwertyuiopasdfghjklzxcvbnmqwertyuiopasdfghjklzxcvbnm", size must be between 0 and 50, size must be between 0 and 50
            """)
        @WithMockUser(roles = "OPERATOR")
        @DisplayName("Should return 400 Bad Request when invalid create user request fields")
        void shouldReturn400_whenInvalidCreateUserRequest(
            String username,
            String password,
            String usernameMessage,
            String passwordMessage
        ) throws Exception{
            //Arrange
            CreateUserRequest request = new CreateUserRequest(username, password);

            //Act and Assert
            ResultActions result = mockMvc.perform(
                    post("/users/operators")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isBadRequest());

            assertSingleErrorValidationErrorResponse(result, Map.of("username", usernameMessage, "password", passwordMessage));
        }

    }

    @Nested
    @DisplayName("changePassword endpoint tests")
    class ChangePasswordTests{

        @Test
        @WithMockUser
        @DisplayName("Should return 204 No Content when successfully changed authenticated user password")
        void shouldReturn204_whenSuccessfullyChangedUserPassword() throws Exception{
            //Arrange
            PasswordChangeRequest request = UserTestDataFactory.DEFAULT_PASSWORD_CHANGE_REQUEST;

            doNothing()
                .when(userService).changePassword(request);

            //Act and Assert
            ResultActions result = mockMvc.perform(
                    patch("/users/password/change")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                );

            assertNoContentResponse(result);

            verify(userService).changePassword(request);
        }

        @Test
        @DisplayName("Should return 401 Unauthorized when accessed with no authentication")
        void shouldReturn401_whenNoAuthentication() throws Exception {
            //Arrange
            PasswordChangeRequest request = UserTestDataFactory.DEFAULT_PASSWORD_CHANGE_REQUEST;

            //Act and Assert
            ResultActions result = mockMvc.perform(
                    patch("/users/password/change")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isUnauthorized());

            assertNoAuthErrorResponse(result);
        }

        @ParameterizedTest
        @CsvSource(
            nullValues = "NULL",
            quoteCharacter = '"',
            textBlock = """
            NULL, NULL, must not be blank, must not be blank
            "", "", must not be blank, must not be blank
            "   ", "   ", must not be blank, must not be blank
            "qwertyuiopasdfghjklzxcvbnmqwertyuiopasdfghjklzxcvbnm", "qwertyuiopasdfghjklzxcvbnmqwertyuiopasdfghjklzxcvbnm", size must be between 0 and 50, size must be between 0 and 50
            """)
        @WithMockUser
        @DisplayName("Should return 400 Bad Request when invalid password change request fields")
        void shouldReturn400_whenInvalidPasswordChangeRequest(
            String oldPassword,
            String newPassword,
            String oldPasswordMessage,
            String newPasswordMessage
        ) throws Exception{
            //Arrange
            PasswordChangeRequest request = new PasswordChangeRequest(oldPassword, newPassword);

            //Act and Assert
            ResultActions result = mockMvc.perform(
                    patch("/users/password/change")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isBadRequest());

            assertSingleErrorValidationErrorResponse(result, Map.of("old_password", oldPasswordMessage, "new_password", newPasswordMessage));
        }

    }

    @Nested
    @DisplayName("deleteUser endpoint tests")
    class DeleteUserTests{

        @Test
        @WithMockUser
        @DisplayName("Should return 204 No Content when successfully deleted authenticated user")
        void shouldReturn204_whenSuccessfullyDeletedUser() throws Exception{
            doNothing()
                .when(userService).deleteUser();

            //Act and Assert
            ResultActions result = mockMvc.perform(
                    delete("/users")
                );

            assertNoContentResponse(result);

            verify(userService).deleteUser();
        }

        @Test
        @DisplayName("Should return 401 Unauthorized when accessed with no authentication")
        void shouldReturn401_whenNoAuthentication() throws Exception {
            //Act and Assert
            ResultActions result = mockMvc.perform(
                    delete("/users")
                )
                .andExpect(status().isUnauthorized());

            assertNoAuthErrorResponse(result);
        }

    }

}
