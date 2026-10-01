package io.github.khaytul_illia.inventory_manager_api.auth;

import io.github.khaytul_illia.inventory_manager_api.TestcontainersConfiguration;
import io.github.khaytul_illia.inventory_manager_api.auth.token.RefreshToken;
import io.github.khaytul_illia.inventory_manager_api.auth.token.RefreshTokenRepository;
import io.github.khaytul_illia.inventory_manager_api.auth.request.LoginRequest;
import io.github.khaytul_illia.inventory_manager_api.auth.request.RefreshTokenRequest;
import io.github.khaytul_illia.inventory_manager_api.auth.response.AccessTokenResponse;
import io.github.khaytul_illia.inventory_manager_api.auth.session.UserSession;
import io.github.khaytul_illia.inventory_manager_api.auth.session.UserSessionRepository;
import io.github.khaytul_illia.inventory_manager_api.auth.token.TokenFactory;
import io.github.khaytul_illia.inventory_manager_api.error.exception.InvalidRefreshTokenException;
import io.github.khaytul_illia.inventory_manager_api.user.User;
import io.github.khaytul_illia.inventory_manager_api.user.UserRepository;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
@DisplayName("AuthService integration tests")
public class AuthServiceIT {

    @MockitoSpyBean
    private TokenFactory tokenFactory;

    @Autowired
    private AuthService authService;
    @Autowired
    private RefreshTokenRepository refreshTokenRepository;
    @Autowired
    private UserSessionRepository sessionRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private TransactionTemplate transactionTemplate;

    private final String password = "password";
    private final Instant now = Instant.now();
    private final String tokenValue = UUID.randomUUID().toString();
    private String hashedTokenValue;
    private User user;
    private UserSession session;
    private RefreshToken refreshToken;

    @BeforeEach
    void beforeEach(){
        hashedTokenValue = tokenFactory.hashTokenValue(tokenValue);

        transactionTemplate.setPropagationBehavior(TransactionTemplate.PROPAGATION_REQUIRES_NEW);
        transactionTemplate.executeWithoutResult(status -> {
            user = userRepository.save(new User(null, "username", passwordEncoder.encode(password), User.UserRole.CUSTOMER));
            session = sessionRepository.save(new UserSession(null, true, now, now.plusSeconds(3600), user));
            refreshToken = refreshTokenRepository.save(new RefreshToken(null, hashedTokenValue, now, false, session, null));
        });
    }

    @AfterEach
    void afterEach(){
        transactionTemplate.executeWithoutResult(status -> {
            refreshTokenRepository.deleteAll();
            sessionRepository.deleteAll();
            userRepository.deleteAll();
        });
    }

    @Nested
    @DisplayName("login integration tests")
    class LoginIT{

        private final LoginRequest request = new LoginRequest("username", password);

        @Test
        @DisplayName("Should roll back all database changes when the refresh token value is not unique")
        void shouldRollBackAllDatabaseChanges_whenCollidingRefreshTokens(){
            //Arrange
            doReturn(hashedTokenValue)
                .when(tokenFactory).hashTokenValue(anyString());

            //Act and Assert
            assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasCauseInstanceOf(ConstraintViolationException.class);

            assertThat(sessionRepository.count()).isEqualTo(1);
            assertThat(refreshTokenRepository.count()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should create new session and return the access token response when refresh token value is unique")
        void shouldCreateSessionAndReturnTokenResponse_whenRefreshTokenIsUnique(){
            //Arrange
            doReturn(hashedTokenValue + "_unique")
                .when(tokenFactory).hashTokenValue(anyString());

            //Act
            AccessTokenResponse response = authService.login(request);

            //Assert
            assertThat(response).isNotNull();
            assertThat(sessionRepository.count()).isEqualTo(2);
            assertThat(refreshTokenRepository.count()).isEqualTo(2);
        }

    }

    @Nested
    @DisplayName("refreshAccess integration tests")
    class RefreshAccessIT{

        private final RefreshTokenRequest request = new RefreshTokenRequest(tokenValue);

        @Test
        @DisplayName("Should invalidate the user session when the refresh token is invalid")
        void shouldInvalidateSession_whenRefreshTokenInvalid(){
            //Arrange
            RefreshToken refreshTokenRef = refreshToken;

            transactionTemplate.executeWithoutResult(status -> {
                RefreshToken sameRefreshToken = refreshTokenRepository.findById(refreshTokenRef.getId()).orElseThrow();

                sameRefreshToken.setUsed(true);
            });

            //Act and Assert
            assertThatThrownBy(() -> authService.refreshAccess(request))
                .isInstanceOf(InvalidRefreshTokenException.class);

            assertThat(sessionRepository.findById(session.getId()).orElseThrow().isValid()).isFalse();
            assertThat(refreshTokenRepository.count()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should throw OptimisticLockingFailureException when the refresh token has been used concurrently")
        void shouldThrowOptimisticLockingFailureException_whenRefreshTokenUsedConcurrently(){
            //Arrange
            RefreshToken refreshTokenRef = refreshToken;

            doAnswer(invocation -> {
                transactionTemplate.executeWithoutResult(status -> {
                    RefreshToken sameRefreshToken = refreshTokenRepository.findById(refreshTokenRef.getId()).orElseThrow();

                    sameRefreshToken.setUsed(true);
                });

                return invocation.callRealMethod();
            })
                .when(tokenFactory).buildRefreshToken(anyString(), any(Instant.class), any(UserSession.class));

            //Act and Assert
            assertThatThrownBy(() -> authService.refreshAccess(request))
                .isInstanceOf(OptimisticLockingFailureException.class);

            assertThat(refreshTokenRepository.findById(refreshToken.getId()).orElseThrow().isUsed()).isTrue();
            assertThat(refreshTokenRepository.count()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should use refresh token and return an access token response when refresh token is valid")
        void shouldUseRefreshTokenAndReturnTokenResponse_whenTheRefreshTokenIsValid(){
            //Arrange
            doCallRealMethod()
                .doReturn(hashedTokenValue + "_unique")
                .when(tokenFactory).hashTokenValue(anyString());

            //Act
            AccessTokenResponse response = authService.refreshAccess(request);

            //Assert
            assertThat(response).isNotNull();
            assertThat(sessionRepository.count()).isEqualTo(1);
            assertThat(refreshTokenRepository.count()).isEqualTo(2);
            assertThat(refreshTokenRepository.findById(refreshToken.getId()).orElseThrow().isUsed()).isTrue();
        }

    }

    @Nested
    @DisplayName("logout integration tests")
    class LogoutIT{

        private final RefreshTokenRequest request = new RefreshTokenRequest(tokenValue);

        @AfterEach
        void afterEach(){
            SecurityContextHolder.getContext().setAuthentication(null);
        }

        @Test
        @DisplayName("Should invalidate user session when refresh token belongs to authenticated user")
        void shouldInvalidateSession_whenRefreshTokenBelongsToAuthenticatedUser(){
            //Arrange
            Jwt jwt = mock(Jwt.class);
            Authentication authentication = new TestingAuthenticationToken(jwt, "password", User.UserRole.CUSTOMER.name());
            SecurityContextHolder.getContext().setAuthentication(authentication);

            when(jwt.getSubject())
                .thenReturn(user.getUsername());

            //Act
            authService.logout(request);

            //Assert
            assertThat(sessionRepository.findById(session.getId()).orElseThrow().isValid()).isFalse();
        }

    }

    @Nested
    @DisplayName("logoutAll integration tests")
    class LogoutAllIT{

        @AfterEach
        void afterEach(){
            SecurityContextHolder.getContext().setAuthentication(null);
        }

        @Test
        @DisplayName("Should invalidate all owned user sessions")
        void shouldInvalidateAllUserSessions(){
            //Arrange
            Jwt jwt = mock(Jwt.class);
            Authentication authentication = new TestingAuthenticationToken(jwt, "password", User.UserRole.CUSTOMER.name());
            SecurityContextHolder.getContext().setAuthentication(authentication);
            UserSession differentSession = sessionRepository.save(new UserSession(null, true, now, now.plusSeconds(3600), user));

            when(jwt.getSubject())
                .thenReturn(user.getUsername());

            //Act
            authService.logoutAll();

            //Assert
            assertThat(sessionRepository.findById(session.getId()).orElseThrow().isValid()).isFalse();
            assertThat(sessionRepository.findById(differentSession.getId()).orElseThrow().isValid()).isFalse();
        }

    }

}
