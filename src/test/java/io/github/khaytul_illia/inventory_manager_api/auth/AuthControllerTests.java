package io.github.khaytul_illia.inventory_manager_api.auth;

import io.github.khaytul_illia.inventory_manager_api.auth.request.LoginRequest;
import io.github.khaytul_illia.inventory_manager_api.auth.request.RefreshTokenRequest;
import io.github.khaytul_illia.inventory_manager_api.auth.response.AccessTokenResponse;
import io.github.khaytul_illia.inventory_manager_api.support.context.ControllerTestContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.ResultActions;

import java.time.Instant;
import java.util.Map;

import static io.github.khaytul_illia.inventory_manager_api.support.utils.mvc.ErrorResponseAssertionUtils.*;
import static io.github.khaytul_illia.inventory_manager_api.support.utils.mvc.SuccessResponseAssertionUtils.assertAccessTokenResponse;
import static io.github.khaytul_illia.inventory_manager_api.support.utils.mvc.SuccessResponseAssertionUtils.assertNoContentResponse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("AuthController tests")
public class AuthControllerTests extends ControllerTestContext {

    @Nested
    @DisplayName("login endpoint tests")
    class LoginTests{

        @Test
        @WithMockUser
        @DisplayName("Should return 200 OK when login is successful")
        void shouldReturn200_whenSuccessfulLogin() throws Exception{
            //Arrange
            LoginRequest request = new LoginRequest(
                "username",
                "password"
            );
            Instant now = Instant.now();
            AccessTokenResponse response = new AccessTokenResponse(
                "issuer",
                now,
                now.plusSeconds(900),
                "username",
                "CUSTOMER",
                "access token",
                "refresh token"
            );

            when(authService.login(any(LoginRequest.class)))
                .thenReturn(response);

            //Act and Assert
            ResultActions result = mockMvc.perform(
                    post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isOk());

            assertAccessTokenResponse(result, response);

            verify(authService).login(any(LoginRequest.class));
        }

        @Test
        @DisplayName("Should return 200 OK when accessed with no authentication")
        void shouldReturn200_whenNoAuthentication() throws Exception{
            //Arrange
            LoginRequest request = new LoginRequest(
                "username",
                "password"
            );
            Instant now = Instant.now();
            AccessTokenResponse response = new AccessTokenResponse(
                "issuer",
                now,
                now.plusSeconds(900),
                "username",
                "CUSTOMER",
                "access token",
                "refresh token"
            );

            when(authService.login(any(LoginRequest.class)))
                .thenReturn(response);

            //Act and Assert
            mockMvc.perform(
                    post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isOk());
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
        @DisplayName("Should return 400 Bad Request when invalid login request fields")
        void shouldReturn400_whenInvalidLoginRequest(
            String username,
            String password,
            String usernameMessage,
            String passwordMessage
        ) throws Exception{
            //Arrange
            LoginRequest request = new LoginRequest(username, password);

            //Act and Assert
            ResultActions result = mockMvc.perform(
                    post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isBadRequest());

            assertSingleErrorValidationErrorResponse(result, Map.of("username", usernameMessage, "password", passwordMessage));

            verify(authService, never()).login(any(LoginRequest.class));
        }

    }

    @Nested
    @DisplayName("refreshAccess endpoint tests")
    class RefreshAccessTests{

        @Test
        @WithMockUser
        @DisplayName("Should return 200 OK when successfully refreshed access")
        void shouldReturn200_whenSuccessfulRefreshAccess() throws Exception{
            //Arrange
            RefreshTokenRequest request = new RefreshTokenRequest("refresh token value");
            Instant now = Instant.now();
            AccessTokenResponse response = new AccessTokenResponse(
                "issuer",
                now,
                now.plusSeconds(900),
                "username",
                "CUSTOMER",
                "access token",
                "new refresh token"
            );

            when(authService.refreshAccess(any(RefreshTokenRequest.class)))
                .thenReturn(response);

            //Act and Assert
            ResultActions result = mockMvc.perform(
                    post("/auth/refresh-access")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isOk());

            assertAccessTokenResponse(result, response);

            verify(authService).refreshAccess(any(RefreshTokenRequest.class));
        }

        @Test
        @DisplayName("Should return 200 OK when accessed with no authentication")
        void shouldReturn200_whenNoAuthentication() throws Exception{
            //Arrange
            RefreshTokenRequest request = new RefreshTokenRequest("refresh token value");
            Instant now = Instant.now();
            AccessTokenResponse response = new AccessTokenResponse(
                "issuer",
                now,
                now.plusSeconds(900),
                "username",
                "CUSTOMER",
                "access token",
                "new refresh token"
            );

            when(authService.refreshAccess(any(RefreshTokenRequest.class)))
                .thenReturn(response);

            //Act and Assert
            mockMvc.perform(
                    post("/auth/refresh-access")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isOk());
        }

        @ParameterizedTest
        @CsvSource(
            nullValues = "NULL",
            quoteCharacter = '"',
            textBlock = """
            NULL, must not be blank
            "", must not be blank
            "   ", must not be blank
            "qwertyuiopasdfghjklzxcvbnmqwertyuiopasdfghjklzxcvbnm", size must be between 0 and 36
            """)
        @WithMockUser
        @DisplayName("Should return 400 Bad Request when invalid refresh token request fields")
        void shouldReturn400_whenInvalidRefreshTokenRequest(
            String refreshToken,
            String refreshTokenMessage
        ) throws Exception{
            //Arrange
            RefreshTokenRequest request = new RefreshTokenRequest(refreshToken);

            //Act and Assert
            ResultActions result = mockMvc.perform(
                    post("/auth/refresh-access")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isBadRequest());

            assertSingleErrorValidationErrorResponse(result, Map.of("refresh_token", refreshTokenMessage));

            verify(authService, never()).refreshAccess(any(RefreshTokenRequest.class));
        }

    }

    @Nested
    @DisplayName("logout endpoint tests")
    class LogoutTests{

        @Test
        @WithMockUser
        @DisplayName("Should return 204 No Content when logout is successful")
        void shouldReturn204_whenSuccessfulLogout() throws Exception{
            //Arrange
            RefreshTokenRequest request = new RefreshTokenRequest("refresh token value");

            doNothing().
                when(authService).logout(any(RefreshTokenRequest.class));

            //Act and Assert
            ResultActions result = mockMvc.perform(
                    post("/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                );

            assertNoContentResponse(result);

            verify(authService).logout(any(RefreshTokenRequest.class));
        }

        @Test
        @DisplayName("Should return 401 Unauthorized when accessed with no authentication")
        void shouldReturn401_whenNoAuthentication() throws Exception{
            //Arrange
            RefreshTokenRequest request = new RefreshTokenRequest("refresh token value");

            doNothing().
                when(authService).logout(any(RefreshTokenRequest.class));

            //Act and Assert
            ResultActions result = mockMvc.perform(
                    post("/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isUnauthorized());

            assertNoAuthErrorResponse(result);

            verify(authService, never()).logout(any(RefreshTokenRequest.class));
        }

        @ParameterizedTest
        @CsvSource(
            nullValues = "NULL",
            quoteCharacter = '"',
            textBlock = """
            NULL, must not be blank
            "", must not be blank
            "   ", must not be blank
            "qwertyuiopasdfghjklzxcvbnmqwertyuiopasdfghjklzxcvbnm", size must be between 0 and 36
            """)
        @WithMockUser
        @DisplayName("Should return 400 Bad Request when invalid refresh token request fields")
        void shouldReturn400_whenInvalidRefreshTokenRequest(
            String refreshToken,
            String refreshTokenMessage
        ) throws Exception{
            //Arrange
            RefreshTokenRequest request = new RefreshTokenRequest(refreshToken);

            //Act and Assert
            ResultActions result = mockMvc.perform(
                    post("/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isBadRequest());

            assertSingleErrorValidationErrorResponse(result, Map.of("refresh_token", refreshTokenMessage));

            verify(authService, never()).logout(any(RefreshTokenRequest.class));
        }

    }

    @Nested
    @DisplayName("logoutAll endpoint tests")
    class LogoutAllTests{

        @Test
        @WithMockUser
        @DisplayName("Should return 204 No Content when successfully logged out of all sessions")
        void shouldReturn204_whenSuccessfulLogoutAll() throws Exception{
            //Arrange
            doNothing().
                when(authService).logoutAll();

            //Act and Assert
            ResultActions result = mockMvc.perform(
                    post("/auth/logout-all")
                );

            assertNoContentResponse(result);

            verify(authService).logoutAll();
        }

        @Test
        @DisplayName("Should return 401 Unauthorized when accessed with no authentication")
        void shouldReturn401_whenNoAuthentication() throws Exception{
            //Arrange
            doNothing().
                when(authService).logoutAll();

            //Act and Assert
            ResultActions result = mockMvc.perform(
                    post("/auth/logout-all")
                )
                .andExpect(status().isUnauthorized());

            assertNoAuthErrorResponse(result);

            verify(authService, never()).logoutAll();
        }

    }

}
