package io.github.khaytul_illia.inventory_manager_api.auth;

import io.github.khaytul_illia.inventory_manager_api.auth.request.LoginRequest;
import io.github.khaytul_illia.inventory_manager_api.auth.response.AccessTokenResponse;
import io.github.khaytul_illia.inventory_manager_api.error.exception.UserSessionLimitExceededException;
import io.github.khaytul_illia.inventory_manager_api.security.login.AppUserDetails;
import io.github.khaytul_illia.inventory_manager_api.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.jwt.Jwt;

import java.net.URI;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService tests")
public class AuthServiceTests {

    private final int maxOpenUserSessions = 10;
    @Mock
    private UserSessionRepository sessionRepository;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private AuthUtils authUtils;
    private AuthService authService;

    @BeforeEach
    void beforeEach(){
        authService = new AuthService(
            maxOpenUserSessions,
            sessionRepository,
            refreshTokenRepository,
            authenticationManager,
            authUtils
        );
    }

    @Nested
    @DisplayName("login tests")
    class LoginTests{

        private final LoginRequest request = new LoginRequest("username", "password");

        @Test
        @DisplayName("Should throw BadCredentialsException when user fails to authenticate")
        void shouldThrowBadCredentialsException_whenUserFailsAuthentication(){
            //Arrange
            AuthenticationException exception = new BadCredentialsException("Failed to authenticate");

            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(exception);

            //Act and Assert
            assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Invalid credentials")
                .hasCause(exception);

            verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        }

        @ParameterizedTest
        @ValueSource(ints = {10, 11})
        @DisplayName("Should throw UserSessionLimitExceededException when user has already opened a max amount of sessions")
        void shouldThrowUserSessionLimitExceededException_whenSessionLimitReached(int openedSessions){
            //Arrange
            User user = new User(1L, "username", "password", User.UserRole.CUSTOMER);
            AppUserDetails userDetails = new AppUserDetails(user);

            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(new UsernamePasswordAuthenticationToken(userDetails, userDetails.getPassword(), userDetails.getAuthorities()));
            when(sessionRepository.countOpenUserSessions(user.getId()))
                .thenReturn(openedSessions);

            //Act and Assert
            assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(UserSessionLimitExceededException.class)
                .hasMessage(String.format("Maximum amount of user sessions opened (%s)", maxOpenUserSessions));

            verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
            verify(sessionRepository).countOpenUserSessions(user.getId());
        }

        @Test
        @DisplayName("Should create new session and return access and refresh tokens when user authenticated successfully")
        void shouldCreateSessionAndReturnTokens_whenUserAuthenticatedSuccessfully() throws Exception {
            //Arrange
            User user = new User(1L, "username", "password", User.UserRole.CUSTOMER);
            AppUserDetails userDetails = new AppUserDetails(user);
            Instant now = Instant.now();
            UserSession session = new UserSession(1L, true, now, now.plusSeconds(3600), user);
            RefreshToken refreshToken = new RefreshToken(1L, "refresh token value", now, false, session, 1);
            Jwt jwt = mock(Jwt.class);
            String issuer = "http://localhost:8080/api/v1";
            Instant expiresAt = now.plusSeconds(900);
            String tokenValue = "access token value";

            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(new UsernamePasswordAuthenticationToken(userDetails, userDetails.getPassword(), userDetails.getAuthorities()));
            when(sessionRepository.countOpenUserSessions(user.getId()))
                .thenReturn(0);
            when(authUtils.buildUserSession(any(Instant.class), any(User.class)))
                .thenReturn(session);
            when(sessionRepository.save(session))
                .thenReturn(session);
            when(authUtils.buildRefreshToken(anyString(), any(Instant.class), any(UserSession.class)))
                .thenReturn(refreshToken);
            when(refreshTokenRepository.save(refreshToken))
                .thenReturn(refreshToken);
            when(authUtils.buildAccessToken(any(Instant.class), any(User.class)))
                .thenReturn(jwt);
            when(jwt.getIssuer()).thenReturn(new URI(issuer).toURL());
            when(jwt.getIssuedAt()).thenReturn(now);
            when(jwt.getExpiresAt()).thenReturn(expiresAt);
            when(jwt.getSubject()).thenReturn(user.getUsername());
            when(jwt.getClaimAsString("roles")).thenReturn(user.getRole().name());
            when(jwt.getTokenValue()).thenReturn(tokenValue);

            //Act
            AccessTokenResponse response = authService.login(request);

            //Assert
            assertThat(response).isNotNull();
            assertThat(response.issuer()).isEqualTo(issuer);
            assertThat(response.issuedAt()).isEqualTo(now);
            assertThat(response.expiresAt()).isEqualTo(expiresAt);
            assertThat(response.subject()).isEqualTo(user.getUsername());
            assertThat(response.role()).isEqualTo(user.getRole().name());
            assertThat(response.accessToken()).isEqualTo(tokenValue);
            assertThat(response.refreshToken()).hasSize(36);

            verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
            verify(sessionRepository).countOpenUserSessions(user.getId());
            verify(authUtils).buildUserSession(any(Instant.class), any(User.class));
            verify(sessionRepository).save(session);
            verify(authUtils).buildRefreshToken(anyString(), any(Instant.class), any(UserSession.class));
            verify(refreshTokenRepository).save(refreshToken);
            verify(authUtils).buildAccessToken(any(Instant.class), any(User.class));
        }

    }

}
