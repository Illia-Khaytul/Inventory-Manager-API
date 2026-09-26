package io.github.khaytul_illia.inventory_manager_api.auth;

import io.github.khaytul_illia.inventory_manager_api.auth.request.LoginRequest;
import io.github.khaytul_illia.inventory_manager_api.auth.request.RefreshTokenRequest;
import io.github.khaytul_illia.inventory_manager_api.auth.response.AccessTokenResponse;
import io.github.khaytul_illia.inventory_manager_api.security.AuthenticationErrorHandler;
import io.github.khaytul_illia.inventory_manager_api.security.AuthorizationErrorHandler;
import io.github.khaytul_illia.inventory_manager_api.security.SecurityConfig;
import io.github.khaytul_illia.inventory_manager_api.security.jwt.JwtAuthenticationErrorHandler;
import io.github.khaytul_illia.inventory_manager_api.security.jwt.JwtConfig;
import io.github.khaytul_illia.inventory_manager_api.security.jwt.JwtRsaPemKeyConfig;
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

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import({
    SecurityConfig.class,
    JwtRsaPemKeyConfig.class,
    JwtConfig.class,
    JwtAuthenticationErrorHandler.class,
    AuthenticationErrorHandler.class,
    AuthorizationErrorHandler.class
})
@ActiveProfiles("test")
@DisplayName("AuthController tests")
public class AuthControllerTests {

    @MockitoBean
    private AuthService authService;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

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
            mockMvc.perform(
                    post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.issuer").value(response.issuer()))
                .andExpect(jsonPath("$.issuedAt").value(response.issuedAt().toString()))
                .andExpect(jsonPath("$.expiresAt").value(response.expiresAt().toString()))
                .andExpect(jsonPath("$.subject").value(response.subject()))
                .andExpect(jsonPath("$.role").value(response.role()))
                .andExpect(jsonPath("$.accessToken").value(response.accessToken()))
                .andExpect(jsonPath("$.refreshToken").value(response.refreshToken()));

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
            mockMvc.perform(
                    post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(HttpServletResponse.SC_BAD_REQUEST))
                .andExpect(jsonPath("$.data.username").value(usernameMessage))
                .andExpect(jsonPath("$.data.password").value(passwordMessage));

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
            mockMvc.perform(
                    post("/auth/refresh-access")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.issuer").value(response.issuer()))
                .andExpect(jsonPath("$.issuedAt").value(response.issuedAt().toString()))
                .andExpect(jsonPath("$.expiresAt").value(response.expiresAt().toString()))
                .andExpect(jsonPath("$.subject").value(response.subject()))
                .andExpect(jsonPath("$.role").value(response.role()))
                .andExpect(jsonPath("$.accessToken").value(response.accessToken()))
                .andExpect(jsonPath("$.refreshToken").value(response.refreshToken()));

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
            "qwertyuiopasdfghjklzxcvbnmqwertyuiopasdfghjklzxcvbnm", size must be between 0 and 50
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
            mockMvc.perform(
                    post("/auth/refresh-access")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(HttpServletResponse.SC_BAD_REQUEST))
                .andExpect(jsonPath("$.data.refreshToken").value(refreshTokenMessage));
        }

    }

}
