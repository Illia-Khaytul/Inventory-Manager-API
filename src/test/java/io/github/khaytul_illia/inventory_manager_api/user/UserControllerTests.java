package io.github.khaytul_illia.inventory_manager_api.user;

import io.github.khaytul_illia.inventory_manager_api.security.AuthenticationErrorHandler;
import io.github.khaytul_illia.inventory_manager_api.security.AuthorizationErrorHandler;
import io.github.khaytul_illia.inventory_manager_api.security.SecurityConfig;
import io.github.khaytul_illia.inventory_manager_api.security.jwt.JwtAuthenticationErrorHandler;
import io.github.khaytul_illia.inventory_manager_api.security.jwt.JwtConfig;
import io.github.khaytul_illia.inventory_manager_api.security.jwt.JwtRsaPemKeyConfig;
import io.github.khaytul_illia.inventory_manager_api.user.request.CreateUserRequest;
import io.github.khaytul_illia.inventory_manager_api.user.response.UserResponse;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@Import({
    SecurityConfig.class,
    JwtRsaPemKeyConfig.class,
    JwtConfig.class,
    JwtAuthenticationErrorHandler.class,
    AuthenticationErrorHandler.class,
    AuthorizationErrorHandler.class
})
@ActiveProfiles("test")
@DisplayName("UserController tests")
public class UserControllerTests {

    @MockitoBean
    private UserService userService;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Nested
    @DisplayName("createCustomer endpoint tests")
    class CreateCustomerTests{

        @Test
        @WithMockUser
        @DisplayName("Should return 201 Created when successfully created new user with CUSTOMER role")
        void shouldReturn201_whenSuccessfullyCreatedCustomer() throws Exception{
            //Arrange
            CreateUserRequest request = new CreateUserRequest("username", "password");
            UserResponse response = new UserResponse(1L, "username", User.UserRole.CUSTOMER);

            when(userService.createUser(request, User.UserRole.CUSTOMER))
                .thenReturn(response);

            //Act and Assert
            mockMvc.perform(
                    post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(response.id()))
                .andExpect(jsonPath("$.username").value(response.username()))
                .andExpect(jsonPath("$.role").value(response.role().name()));

            verify(userService).createUser(request, User.UserRole.CUSTOMER);
        }

        @Test
        @DisplayName("Should return 201 Created when accessed with no authentication")
        void shouldReturn201_whenNoAuthentication() throws Exception {
            //Arrange
            CreateUserRequest request = new CreateUserRequest("username", "password");
            UserResponse response = new UserResponse(1L, "username", User.UserRole.CUSTOMER);

            when(userService.createUser(request, User.UserRole.CUSTOMER))
                .thenReturn(response);

            //Act and Assert
            mockMvc.perform(
                    post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(response.id()));

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
            mockMvc.perform(
                    post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(HttpServletResponse.SC_BAD_REQUEST))
                .andExpect(jsonPath("$.data.username").value(usernameMessage))
                .andExpect(jsonPath("$.data.password").value(passwordMessage));
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
            CreateUserRequest request = new CreateUserRequest("username", "password");
            UserResponse response = new UserResponse(1L, "username", User.UserRole.OPERATOR);

            when(userService.createUser(request, User.UserRole.OPERATOR))
                .thenReturn(response);

            //Act and Assert
            mockMvc.perform(
                    post("/users/operators")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(response.id()))
                .andExpect(jsonPath("$.username").value(response.username()))
                .andExpect(jsonPath("$.role").value(response.role().name()));

            verify(userService).createUser(request, User.UserRole.OPERATOR);
        }

        @Test
        @WithMockUser(roles = "CUSTOMER")
        @DisplayName("Should return 403 Forbidden when accessed with authentication but wrong´role")
        void shouldReturn403_whenAuthenticatedWithWrongRole() throws Exception {
            //Arrange
            CreateUserRequest request = new CreateUserRequest("username", "password");

            //Act and Assert
            mockMvc.perform(
                    post("/users/operators")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(HttpServletResponse.SC_FORBIDDEN));
        }

        @Test
        @DisplayName("Should return 401 Unauthorized when accessed with no authentication")
        void shouldReturn401_whenNoAuthentication() throws Exception {
            //Arrange
            CreateUserRequest request = new CreateUserRequest("username", "password");

            //Act and Assert
            mockMvc.perform(
                    post("/users/operators")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(HttpServletResponse.SC_UNAUTHORIZED));
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
            mockMvc.perform(
                    post("/users/operators")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(HttpServletResponse.SC_BAD_REQUEST))
                .andExpect(jsonPath("$.data.username").value(usernameMessage))
                .andExpect(jsonPath("$.data.password").value(passwordMessage));
        }

    }

}
