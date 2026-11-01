package io.github.khaytul_illia.inventory_manager_api.auth.token;

import io.github.khaytul_illia.inventory_manager_api.auth.response.AccessTokenResponse;
import io.github.khaytul_illia.inventory_manager_api.auth.session.UserSession;
import io.github.khaytul_illia.inventory_manager_api.auth.session.UserSessionService;
import io.github.khaytul_illia.inventory_manager_api.error.exception.InvalidRefreshTokenException;
import io.github.khaytul_illia.inventory_manager_api.support.data.RefreshTokenTestDataFactory;
import io.github.khaytul_illia.inventory_manager_api.support.data.TestDataConstants;
import io.github.khaytul_illia.inventory_manager_api.support.data.UserSessionTestDataFactory;
import io.github.khaytul_illia.inventory_manager_api.support.data.UserTestDataFactory;
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

        private final RefreshToken refreshToken = RefreshTokenTestDataFactory.existingDefaultRefreshToken();
        private final UserSession session = refreshToken.getSession();
        private final User user = session.getUser();

        @Test
        @DisplayName("Should create new access and refresh tokens")
        void shouldCreateNewAccessAndRefreshTokens() throws Exception {
            //Arrange
            Instant createdAt = Instant.now();
            String username = user.getUsername();
            Jwt accessTokenMock = mock(Jwt.class);
            String issuer = "http://localhost:8080/api/v1";
            Instant expiresAt = createdAt.plusSeconds(900);
            String accessTokenValue = TestDataConstants.ACCESS_TOKEN_VALUE;

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
                .thenReturn(username);
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
            assertThat(response.subject()).isEqualTo(username);
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

        private final String tokenValue = TestDataConstants.REFRESH_TOKEN_VALUE;
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
            RefreshToken foundRefreshToken = RefreshTokenTestDataFactory.existingDefaultRefreshToken();

            doReturn(foundRefreshToken)
                .when(tokenServiceSpy).loadRefreshToken(tokenValue);
            when(refreshTokenRepository.saveAndFlush(foundRefreshToken))
                .thenReturn(foundRefreshToken);

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
            User user = UserTestDataFactory.existingDefaultUser();
            UserSession validSession = UserSessionTestDataFactory.existingUserSession(user);
            UserSession expiredSession = UserSessionTestDataFactory.existingUserSession(true, Instant.now().minusSeconds(36000), user);

            return Stream.of(
                //Refresh token is used
                Arguments.of(
                    RefreshTokenTestDataFactory.existingUniqueRefreshToken(true, validSession),
                    List.of("Detected refresh token reuse")
                ),
                //Refresh token session is expired
                Arguments.of(
                    RefreshTokenTestDataFactory.existingUniqueRefreshToken(expiredSession),
                    List.of("Attempted access to an expired session")
                )
            );
        }

        static Stream<Arguments> provideInvalidSessionRefreshTokens(){
            Instant now = Instant.now();
            User user = UserTestDataFactory.existingDefaultUser();
            UserSession invalidSession = UserSessionTestDataFactory.existingUserSession(false, now, user);
            UserSession invalidAndExpiredSession = UserSessionTestDataFactory.existingUserSession(false, now.minusSeconds(36000), user);

            return Stream.of(
                //Refresh token session is invalid
                Arguments.of(
                    RefreshTokenTestDataFactory.existingUniqueRefreshToken(invalidSession),
                    List.of("Attempted access to an invalidated session")
                ),
                //Refresh token is used and session is invalid and expired
                Arguments.of(
                    RefreshTokenTestDataFactory.existingUniqueRefreshToken(true, invalidAndExpiredSession),
                    List.of("Detected refresh token reuse", "Attempted access to an invalidated session", "Attempted access to an expired session")
                )
            );
        }

    }

    @Nested
    @DisplayName("loadRefreshToken tests")
    class LoadRefreshTokenTests{

        private final String tokenValue = TestDataConstants.REFRESH_TOKEN_VALUE;

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
            RefreshToken foundRefreshToken = RefreshTokenTestDataFactory.existingDefaultRefreshToken();

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
