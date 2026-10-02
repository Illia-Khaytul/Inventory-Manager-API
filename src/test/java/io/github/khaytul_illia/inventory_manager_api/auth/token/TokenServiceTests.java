package io.github.khaytul_illia.inventory_manager_api.auth.token;

import io.github.khaytul_illia.inventory_manager_api.auth.response.AccessTokenResponse;
import io.github.khaytul_illia.inventory_manager_api.auth.session.UserSession;
import io.github.khaytul_illia.inventory_manager_api.auth.session.UserSessionService;
import io.github.khaytul_illia.inventory_manager_api.error.exception.InvalidRefreshTokenException;
import io.github.khaytul_illia.inventory_manager_api.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserSessionFactory tests")
public class TokenServiceTests {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private UserSessionService sessionService;
    @Mock
    private TokenFactory tokenFactory;
    @InjectMocks
    private TokenService tokenService;

    @Nested
    @DisplayName("createAccessRefreshTokenPair tests")
    class CreateAccessRefreshTokenPairTests{

        private final Instant createdAt = Instant.now();
        private final User user = new User(1L, "username", "password", User.UserRole.CUSTOMER);
        private final UserSession session = new UserSession(1L, true, createdAt, createdAt.plusSeconds(3600), user);

        @Test
        @DisplayName("Should create new access and refresh tokens")
        void shouldCreateNewAccessAndRefreshTokens() throws Exception {
            //Arrange
            RefreshToken refreshToken = new RefreshToken(1L, "refresh token value", createdAt, false, session, 1);
            Jwt accessTokenMock = mock(Jwt.class);
            String issuer = "http://localhost:8080/api/v1";
            Instant expiresAt = createdAt.plusSeconds(900);
            String accessTokenValue = "access token value";

            when(tokenFactory.buildRefreshToken(anyString(), any(Instant.class), any(UserSession.class)))
                .thenReturn(refreshToken);
            when(refreshTokenRepository.save(refreshToken))
                .thenReturn(refreshToken);
            when(tokenFactory.buildAccessToken(any(Instant.class), any(User.class)))
                .thenReturn(accessTokenMock);
            when(accessTokenMock.getIssuer())
                .thenReturn(URI.create(issuer).toURL());
            when(accessTokenMock.getIssuedAt())
                .thenReturn(createdAt);
            when(accessTokenMock.getExpiresAt())
                .thenReturn(expiresAt);
            when(accessTokenMock.getSubject())
                .thenReturn(user.getUsername());
            when(accessTokenMock.getClaimAsString("roles"))
                .thenReturn(user.getRole().name());
            when(accessTokenMock.getTokenValue())
                .thenReturn(accessTokenValue);

            //Act
            AccessTokenResponse response = tokenService.createAccessRefreshTokenPair(session);

            //Assert
            assertThat(response).isNotNull();
            assertThat(response.issuer()).isEqualTo(issuer);
            assertThat(response.issuedAt()).isEqualTo(createdAt);
            assertThat(response.expiresAt()).isEqualTo(expiresAt);
            assertThat(response.subject()).isEqualTo(user.getUsername());
            assertThat(response.role()).isEqualTo(user.getRole().name());
            assertThat(response.accessToken()).isEqualTo(accessTokenValue);
            assertThat(response.refreshToken()).hasSize(36);

            verify(tokenFactory).buildRefreshToken(anyString(), any(Instant.class), any(UserSession.class));
            verify(refreshTokenRepository).save(refreshToken);
            verify(tokenFactory).buildAccessToken(any(Instant.class), any(User.class));
        }

    }

    @Nested
    @DisplayName("useRefreshToken tests")
    class UseRefreshTokenTests{

        private final String tokenValue = "token value";
        private TokenService tokenServiceSpy;

        @BeforeEach
        void beforeEach(){
            tokenServiceSpy = spy(tokenService);
        }

        @ParameterizedTest
        @MethodSource("provideValidSessionInvalidRefreshTokens")
        @DisplayName("Should throw InvalidRefreshTokenException when refresh token is used, or session is expired")
        void shouldThrowInvalidRefreshTokenException_whenRefreshTokenIsInvalid(RefreshToken refreshToken, List<String> expectedDetails){
            //Arrange
            doReturn(refreshToken)
                .when(tokenServiceSpy).loadRefreshToken(tokenValue);
            doNothing()
                .when(sessionService).invalidateSession(refreshToken.getSession().getId());

            //Act and Assert
            assertThatThrownBy(() -> tokenServiceSpy.useRefreshToken(tokenValue))
                .isInstanceOf(InvalidRefreshTokenException.class)
                .hasMessage("Refresh token is used or session is invalid or expired")
                .satisfies(e -> {
                    InvalidRefreshTokenException exception = (InvalidRefreshTokenException) e;
                    assertThat(exception.getDetails()).containsExactlyInAnyOrderElementsOf(expectedDetails);
                });

            verify(tokenServiceSpy).loadRefreshToken(tokenValue);
            verify(sessionService).invalidateSession(refreshToken.getSession().getId());
        }

        @ParameterizedTest
        @MethodSource("provideInvalidSessionRefreshTokens")
        @DisplayName("Should throw InvalidRefreshTokenException when user session is invalid")
        void shouldThrowInvalidRefreshTokenException_whenUserSessionIsInvalid(RefreshToken refreshToken, List<String> expectedDetails){
            //Arrange
            doReturn(refreshToken)
                .when(tokenServiceSpy).loadRefreshToken(tokenValue);

            //Act and Assert
            assertThatThrownBy(() -> tokenServiceSpy.useRefreshToken(tokenValue))
                .isInstanceOf(InvalidRefreshTokenException.class)
                .hasMessage("Refresh token is used or session is invalid or expired")
                .satisfies(e -> {
                    InvalidRefreshTokenException exception = (InvalidRefreshTokenException) e;
                    assertThat(exception.getDetails()).containsExactlyInAnyOrderElementsOf(expectedDetails);
                });

            verify(tokenServiceSpy).loadRefreshToken(tokenValue);
            verify(sessionService, never()).invalidateSession(refreshToken.getSession().getId());
        }

        @Test
        @DisplayName("Should mark the refresh token as used when refresh token is valid")
        void shouldUseRefreshToken_whenRefreshTokenIsValid(){
            //Arrange
            Instant now = Instant.now();
            User user = new User(1L, "username", "password", User.UserRole.CUSTOMER);
            UserSession session = new UserSession(1L, true, now, now.plusSeconds(3600), user);
            RefreshToken foundRefreshToken = new RefreshToken(1L, "refresh token value", now, false, session, 1);

            doReturn(foundRefreshToken)
                .when(tokenServiceSpy).loadRefreshToken(tokenValue);

            //Act
            RefreshToken refreshToken = tokenServiceSpy.useRefreshToken(tokenValue);

            //Assert
            assertThat(refreshToken).isNotNull();
            assertThat(refreshToken.getId()).isEqualTo(foundRefreshToken.getId());
            assertThat(refreshToken.isUsed()).isTrue();

            verify(tokenServiceSpy).loadRefreshToken(tokenValue);
            verify(sessionService, never()).invalidateSession(refreshToken.getSession().getId());
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

    @Nested
    @DisplayName("loadRefreshToken tests")
    class LoadRefreshTokenTests{

        private final String tokenValue = "token value";

        @Test
        @DisplayName("Should throw InvalidRefreshTokenException when refresh token is not found")
        void shouldThrowInvalidRefreshTokenException_whenRefreshTokenIsNotFound(){
            //Arrange
            when(tokenFactory.hashTokenValue(tokenValue))
                .thenReturn(tokenValue);
            when(refreshTokenRepository.findByTokenValue(tokenValue))
                .thenReturn(Optional.empty());

            //Act and Assert
            assertThatThrownBy(() -> tokenService.loadRefreshToken(tokenValue))
                .isInstanceOf(InvalidRefreshTokenException.class)
                .hasMessage("Refresh token is used or session is invalid or expired")
                .satisfies(e -> {
                    InvalidRefreshTokenException exception = (InvalidRefreshTokenException) e;
                    assertThat(exception.getDetails()).containsExactly("Refresh token does not exist");
                });

            verify(tokenFactory).hashTokenValue(tokenValue);
            verify(refreshTokenRepository).findByTokenValue(tokenValue);
        }

        @Test
        @DisplayName("Should return found refresh token when it exists by token value")
        void shouldReturnRefreshToken_whenItExists(){
            //Arrange
            Instant now = Instant.now();
            User user = new User(1L, "username", "password", User.UserRole.CUSTOMER);
            UserSession session = new UserSession(1L, true, now, now.plusSeconds(3600), user);
            RefreshToken foundRefreshToken = new RefreshToken(1L, "refresh token value", now, false, session, 1);

            when(tokenFactory.hashTokenValue(tokenValue))
                .thenReturn(tokenValue);
            when(refreshTokenRepository.findByTokenValue(tokenValue))
                .thenReturn(Optional.of(foundRefreshToken));

            //Act
            RefreshToken refreshToken = tokenService.loadRefreshToken(tokenValue);

            //Assert
            assertThat(refreshToken).isNotNull();
            assertThat(refreshToken.getId()).isEqualTo(foundRefreshToken.getId());

            verify(tokenFactory).hashTokenValue(tokenValue);
            verify(refreshTokenRepository).findByTokenValue(tokenValue);
        }

    }

}
