package io.github.khaytul_illia.inventory_manager_api.auth;

import io.github.khaytul_illia.inventory_manager_api.auth.request.LoginRequest;
import io.github.khaytul_illia.inventory_manager_api.auth.request.RefreshTokenRequest;
import io.github.khaytul_illia.inventory_manager_api.auth.response.AccessTokenResponse;
import io.github.khaytul_illia.inventory_manager_api.error.exception.FailedLoginAuthenticationException;
import io.github.khaytul_illia.inventory_manager_api.error.exception.InvalidRefreshTokenException;
import io.github.khaytul_illia.inventory_manager_api.error.exception.UserSessionLimitExceededException;
import io.github.khaytul_illia.inventory_manager_api.security.login.AppUserDetails;
import io.github.khaytul_illia.inventory_manager_api.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
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
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

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
                .isInstanceOf(FailedLoginAuthenticationException.class)
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

    @Nested
    @DisplayName("refreshAccess tests")
    class RefreshAccessTests{

        @Test
        @DisplayName("Should throw InvalidRefreshTokenException when refresh token is not found")
        void shouldThrowInvalidRefreshTokenException_whenRefreshTokenIsNotFound(){
            //Arrange
            String tokenValue = "refresh token value";
            RefreshTokenRequest request = new RefreshTokenRequest(tokenValue);

            when(authUtils.hashTokenValue(tokenValue))
                .thenReturn(tokenValue);
            when(refreshTokenRepository.findByTokenValue(tokenValue))
                .thenReturn(Optional.empty());

            //Act and Assert
            assertThatThrownBy(() -> authService.refreshAccess(request))
                .isInstanceOf(InvalidRefreshTokenException.class)
                .hasMessage("Refresh token is used or session is invalid or expired")
                .satisfies(e -> {
                    InvalidRefreshTokenException exception = (InvalidRefreshTokenException) e;
                    assertThat(exception.getDetails()).containsExactly("Refresh token does not exist");
                });

            verify(authUtils).hashTokenValue(tokenValue);
            verify(refreshTokenRepository).findByTokenValue(tokenValue);
        }

        @ParameterizedTest
        @MethodSource("provideValidSessionInvalidRefreshTokens")
        @DisplayName("Should throw InvalidRefreshTokenException when refresh token is used, or session is expired")
        void shouldThrowInvalidRefreshTokenException_whenRefreshTokenIsInvalid(RefreshToken refreshToken, List<String> expectedDetails){
            //Arrange
            String tokenValue = refreshToken.getTokenValue();
            RefreshTokenRequest request = new RefreshTokenRequest(tokenValue);

            when(authUtils.hashTokenValue(tokenValue))
                .thenReturn(tokenValue);
            when(refreshTokenRepository.findByTokenValue(tokenValue))
                .thenReturn(Optional.of(refreshToken));
            doNothing()
                .when(sessionRepository).invalidateSessionById(refreshToken.getSession().getId());

            //Act and Assert
            assertThatThrownBy(() -> authService.refreshAccess(request))
                .isInstanceOf(InvalidRefreshTokenException.class)
                .hasMessage("Refresh token is used or session is invalid or expired")
                .satisfies(e -> {
                    InvalidRefreshTokenException exception = (InvalidRefreshTokenException) e;
                    assertThat(exception.getDetails()).containsExactlyInAnyOrderElementsOf(expectedDetails);
                });

            verify(authUtils).hashTokenValue(tokenValue);
            verify(refreshTokenRepository).findByTokenValue(tokenValue);
            verify(sessionRepository).invalidateSessionById(refreshToken.getSession().getId());
        }

        @ParameterizedTest
        @MethodSource("provideInvalidSessionRefreshTokens")
        @DisplayName("Should throw InvalidRefreshTokenException when user session is invalid")
        void shouldThrowInvalidRefreshTokenException_whenUserSessionIsInvalid(RefreshToken refreshToken, List<String> expectedDetails){
            //Arrange
            String tokenValue = refreshToken.getTokenValue();
            RefreshTokenRequest request = new RefreshTokenRequest(tokenValue);

            when(authUtils.hashTokenValue(tokenValue))
                .thenReturn(tokenValue);
            when(refreshTokenRepository.findByTokenValue(tokenValue))
                .thenReturn(Optional.of(refreshToken));

            //Act and Assert
            assertThatThrownBy(() -> authService.refreshAccess(request))
                .isInstanceOf(InvalidRefreshTokenException.class)
                .hasMessage("Refresh token is used or session is invalid or expired")
                .satisfies(e -> {
                    InvalidRefreshTokenException exception = (InvalidRefreshTokenException) e;
                    assertThat(exception.getDetails()).containsExactlyInAnyOrderElementsOf(expectedDetails);
                });

            verify(authUtils).hashTokenValue(tokenValue);
            verify(refreshTokenRepository).findByTokenValue(tokenValue);
            verify(sessionRepository, never()).invalidateSessionById(refreshToken.getSession().getId());
        }

        @Test
        @DisplayName("Should use refresh token and return new access and refresh tokens when provided refresh token is valid")
        void shouldUseRefreshTokenAndReturnNewTokens_whenRefreshTokenIsValid() throws Exception {
            //Arrange
            String tokenValue = "refresh token value";
            RefreshTokenRequest request = new RefreshTokenRequest(tokenValue);
            Instant now = Instant.now();
            User user = new User(1L, "username", "password", User.UserRole.CUSTOMER);
            UserSession session = new UserSession(1L, true, now, now.plusSeconds(3600), user);
            RefreshToken refreshToken = new RefreshToken(1L, "refresh token value", now, false, session, 1);
            RefreshToken newRefreshToken = new RefreshToken(1L, "refresh token value", now, false, session, 1);
            Jwt jwt = mock(Jwt.class);
            String issuer = "http://localhost:8080/api/v1";
            Instant expiresAt = now.plusSeconds(900);
            String accessTokenValue = "access token value";

            when(authUtils.hashTokenValue(tokenValue))
                .thenReturn(tokenValue);
            when(refreshTokenRepository.findByTokenValue(tokenValue))
                .thenReturn(Optional.of(refreshToken));
            when(authUtils.buildRefreshToken(anyString(), any(Instant.class), any(UserSession.class)))
                .thenReturn(newRefreshToken);
            when(refreshTokenRepository.save(newRefreshToken))
                .thenReturn(newRefreshToken);
            when(authUtils.buildAccessToken(any(Instant.class), any(User.class)))
                .thenReturn(jwt);
            when(jwt.getIssuer()).thenReturn(new URI(issuer).toURL());
            when(jwt.getIssuedAt()).thenReturn(now);
            when(jwt.getExpiresAt()).thenReturn(expiresAt);
            when(jwt.getSubject()).thenReturn(user.getUsername());
            when(jwt.getClaimAsString("roles")).thenReturn(user.getRole().name());
            when(jwt.getTokenValue()).thenReturn(accessTokenValue);

            //Act
            AccessTokenResponse response = authService.refreshAccess(request);

            //Assert
            assertThat(response).isNotNull();
            assertThat(response.issuer()).isEqualTo(issuer);
            assertThat(response.issuedAt()).isEqualTo(now);
            assertThat(response.expiresAt()).isEqualTo(expiresAt);
            assertThat(response.subject()).isEqualTo(user.getUsername());
            assertThat(response.role()).isEqualTo(user.getRole().name());
            assertThat(response.accessToken()).isEqualTo(accessTokenValue);
            assertThat(response.refreshToken()).hasSize(36);

            assertThat(refreshToken.isUsed()).isTrue();

            verify(authUtils).hashTokenValue(tokenValue);
            verify(refreshTokenRepository).findByTokenValue(tokenValue);
            verify(sessionRepository, never()).invalidateSessionById(refreshToken.getSession().getId());
            verify(authUtils).buildRefreshToken(anyString(), any(Instant.class), any(UserSession.class));
            verify(refreshTokenRepository).save(newRefreshToken);
            verify(authUtils).buildAccessToken(any(Instant.class), any(User.class));
        }

        /*
                Test data provider methods
         */

        static Stream<Arguments> provideValidSessionInvalidRefreshTokens(){
            Instant now = Instant.now();
            User user = new User(1L, "username", "password", User.UserRole.CUSTOMER);
            UserSession validSession = new UserSession(1L, true, now, now.plusSeconds(3600), user);
            UserSession expiredSession = new UserSession(1L, true, now, now.minusSeconds(3600), user);

            return Stream.of(
                //Refresh token is used
                Arguments.of(
                    new RefreshToken(1L, "refresh token value", now, true, validSession, 1),
                    List.of("Detected refresh token reuse")
                ),
                //Refresh token session is expired
                Arguments.of(
                    new RefreshToken(1L, "refresh token value", now, false, expiredSession, 1),
                    List.of("Attempted access to an expired session")
                )
            );
        }

        static Stream<Arguments> provideInvalidSessionRefreshTokens(){
            Instant now = Instant.now();
            User user = new User(1L, "username", "password", User.UserRole.CUSTOMER);
            UserSession invalidSession = new UserSession(1L, false, now, now.plusSeconds(3600), user);
            UserSession invalidAndExpiredSession = new UserSession(1L, false, now, now.minusSeconds(3600), user);

            return Stream.of(
                //Refresh token session is invalid
                Arguments.of(
                    new RefreshToken(1L, "refresh token value", now, false, invalidSession, 1),
                    List.of("Attempted access to an invalidated session")
                ),
                //Refresh token is used and session is invalid and expired
                Arguments.of(
                    new RefreshToken(1L, "refresh token value", now, true, invalidAndExpiredSession, 1),
                    List.of("Detected refresh token reuse", "Attempted access to an invalidated session", "Attempted access to an expired session")
                )
            );
        }

    }

}
