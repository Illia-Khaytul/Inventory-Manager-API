package io.github.khaytul_illia.inventory_manager_api.auth;

import io.github.khaytul_illia.inventory_manager_api.auth.session.UserSessionService;
import io.github.khaytul_illia.inventory_manager_api.auth.token.RefreshToken;
import io.github.khaytul_illia.inventory_manager_api.auth.request.LoginRequest;
import io.github.khaytul_illia.inventory_manager_api.auth.request.RefreshTokenRequest;
import io.github.khaytul_illia.inventory_manager_api.auth.response.AccessTokenResponse;
import io.github.khaytul_illia.inventory_manager_api.auth.session.UserSession;
import io.github.khaytul_illia.inventory_manager_api.auth.token.TokenService;
import io.github.khaytul_illia.inventory_manager_api.error.exception.FailedLoginAuthenticationException;
import io.github.khaytul_illia.inventory_manager_api.error.exception.InvalidRefreshTokenException;
import io.github.khaytul_illia.inventory_manager_api.security.SecurityUtils;
import io.github.khaytul_illia.inventory_manager_api.security.login.AppUserDetails;
import io.github.khaytul_illia.inventory_manager_api.user.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService tests")
public class AuthServiceTests {

    @Mock
    private UserSessionService sessionService;
    @Mock
    private TokenService tokenService;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private SecurityUtils securityUtils;
    @InjectMocks
    private AuthService authService;

    @Nested
    @DisplayName("login tests")
    class LoginTests{

        private final LoginRequest request = new LoginRequest("username", "password");

        @Test
        @DisplayName("Should throw FailedLoginAuthenticationException when user fails to authenticate")
        void shouldThrowFailedLoginAuthenticationException_whenUserFailsAuthentication(){
            //Arrange
            AuthenticationException exception = new BadCredentialsException("Failed to authenticate");

            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(exception);

            //Act and Assert
            assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(FailedLoginAuthenticationException.class)
                .hasMessage("Invalid credentials")
                .hasCause(exception);

            verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        }

        @Test
        @DisplayName("Should create new session and return access and refresh tokens when user authenticated successfully")
        void shouldCreateSessionAndReturnTokens_whenUserAuthenticatedSuccessfully() {
            //Arrange
            User user = new User(1L, request.username(), request.password(), User.UserRole.CUSTOMER);
            AppUserDetails userDetails = new AppUserDetails(user);
            Instant now = Instant.now();
            UserSession session = new UserSession(1L, true, now, now.plusSeconds(3600), user);
            String issuer = "http://localhost:8080/api/v1";
            String accessTokenValue = "access token value";
            AccessTokenResponse accessTokenResponse = new AccessTokenResponse(
                issuer,
                now,
                now.plusSeconds(900),
                user.getUsername(),
                user.getRole().name(),
                accessTokenValue,
                "refresh token value"
            );

            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(new UsernamePasswordAuthenticationToken(userDetails, userDetails.getPassword(), userDetails.getAuthorities()));
            when(sessionService.createUserSession(user.getId()))
                .thenReturn(session);
            when(tokenService.createAccessRefreshTokenPair(session))
                .thenReturn(accessTokenResponse);

            //Act
            AccessTokenResponse response = authService.login(request);

            //Assert
            assertThat(response).isNotNull();
            assertThat(response.subject()).isEqualTo(accessTokenResponse.subject());
            assertThat(response.issuedAt()).isEqualTo(accessTokenResponse.issuedAt());

            verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
            verify(sessionService).createUserSession(user.getId());
            verify(tokenService).createAccessRefreshTokenPair(session);
        }

    }

    @Nested
    @DisplayName("refreshAccess tests")
    class RefreshAccessTests{

        private final RefreshTokenRequest request = new RefreshTokenRequest("refresh token value");

        @Test
        @DisplayName("Should use refresh token and return new access and refresh tokens")
        void shouldUseRefreshTokenAndReturnNewTokens() {
            //Arrange
            Instant now = Instant.now();
            User user = new User(1L, "username", "password", User.UserRole.CUSTOMER);
            UserSession session = new UserSession(1L, true, now, now.plusSeconds(3600), user);
            RefreshToken refreshToken = new RefreshToken(1L, request.refreshToken(), now, true, session, 1);
            String issuer = "http://localhost:8080/api/v1";
            String accessTokenValue = "access token value";
            AccessTokenResponse accessTokenResponse = new AccessTokenResponse(
                issuer,
                now,
                now.plusSeconds(900),
                user.getUsername(),
                user.getRole().name(),
                accessTokenValue,
                refreshToken.getTokenValue()
            );

            when(tokenService.useRefreshToken(request.refreshToken()))
                .thenReturn(refreshToken);
            when(tokenService.createAccessRefreshTokenPair(session))
                .thenReturn(accessTokenResponse);

            //Act
            AccessTokenResponse response = authService.refreshAccess(request);

            //Assert
            assertThat(response).isNotNull();
            assertThat(response.subject()).isEqualTo(accessTokenResponse.subject());
            assertThat(response.issuedAt()).isEqualTo(accessTokenResponse.issuedAt());

            verify(tokenService).useRefreshToken(request.refreshToken());
            verify(tokenService).createAccessRefreshTokenPair(session);
        }

    }

    @Nested
    @DisplayName("logout tests")
    class LogoutTests{

        private final RefreshTokenRequest request = new RefreshTokenRequest("refresh token value");

        @Test
        @DisplayName("Should return nothing when refresh token is not found by token value")
        void shouldReturnNothing_whenRefreshTokenNotFound(){
            //Arrange
            when(tokenService.loadRefreshToken(request.refreshToken()))
                .thenThrow(new InvalidRefreshTokenException("message", List.of()));

            //Act
            authService.logout(request);

            //Assert
            verify(tokenService).loadRefreshToken(request.refreshToken());
            verify(sessionService, never()).invalidateSession(anyLong());
        }

        @Test
        @DisplayName("Should return nothing when user session does not belong to the authenticated user")
        void shouldReturnNothing_whenSessionDoesNotBelongToAuthenticatedUser(){
            //Arrange
            Instant now = Instant.now();
            User user = new User(1L, "username", "password", User.UserRole.CUSTOMER);
            UserSession session = new UserSession(1L, true, now, now.plusSeconds(3600), user);
            RefreshToken refreshToken = new RefreshToken(1L, "refresh token value", now, false, session, 1);
            Jwt jwt = mock(Jwt.class);

            when(tokenService.loadRefreshToken(request.refreshToken()))
                .thenReturn(refreshToken);
            when(securityUtils.getAuthenticatedUserAccessToken())
                .thenReturn(jwt);
            when(jwt.getSubject())
                .thenReturn(user.getUsername() + "_different");

            //Act
            authService.logout(request);

            //Assert
            verify(tokenService).loadRefreshToken(request.refreshToken());
            verify(securityUtils).getAuthenticatedUserAccessToken();
            verify(sessionService, never()).invalidateSession(anyLong());
        }

        @Test
        @DisplayName("Should return nothing and invalidate session when session belongs to authenticated user")
        void shouldReturnNothingAndInvalidateSession_whenSessionBelongsToAuthenticatedUser(){
            //Arrange
            Instant now = Instant.now();
            User user = new User(1L, "username", "password", User.UserRole.CUSTOMER);
            UserSession session = new UserSession(1L, true, now, now.plusSeconds(3600), user);
            RefreshToken refreshToken = new RefreshToken(1L, "refresh token value", now, false, session, 1);
            Jwt jwt = mock(Jwt.class);

            when(tokenService.loadRefreshToken(request.refreshToken()))
                .thenReturn(refreshToken);
            when(securityUtils.getAuthenticatedUserAccessToken())
                .thenReturn(jwt);
            when(jwt.getSubject())
                .thenReturn(user.getUsername());
            doNothing()
                .when(sessionService).invalidateSession(session.getId());

            //Act
            authService.logout(request);

            //Assert
            verify(tokenService).loadRefreshToken(request.refreshToken());
            verify(securityUtils).getAuthenticatedUserAccessToken();
            verify(sessionService).invalidateSession(anyLong());
        }

    }

    @Nested
    @DisplayName("logoutAll tests")
    class LogoutAllTests{

        @Test
        @DisplayName("Should return nothing and invalidate all user sessions")
        void shouldReturnNothingAndInvalidateAllUserSessions(){
            //Arrange
            String username = "username";
            Jwt jwt = mock(Jwt.class);

            when(securityUtils.getAuthenticatedUserAccessToken())
                .thenReturn(jwt);
            when(jwt.getSubject())
                .thenReturn(username);
            doNothing()
                .when(sessionService).invalidateAllUserSessions(username);

            //Act
            authService.logoutAll();

            //Assert
            verify(securityUtils).getAuthenticatedUserAccessToken();
            verify(sessionService).invalidateAllUserSessions(username);
        }

    }

}
