package io.github.khaytul_illia.inventory_manager_api.auth;

import io.github.khaytul_illia.inventory_manager_api.TestcontainersConfiguration;
import io.github.khaytul_illia.inventory_manager_api.auth.request.LoginRequest;
import io.github.khaytul_illia.inventory_manager_api.auth.request.RefreshTokenRequest;
import io.github.khaytul_illia.inventory_manager_api.auth.response.AccessTokenResponse;
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
import org.springframework.security.crypto.password.PasswordEncoder;
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
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("AuthService integration tests")
public class AuthServiceIT {

    @MockitoSpyBean
    private AuthUtils authUtils;

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
    private String encodedPassword;
    private final Instant now = Instant.now();
    private final String tokenValue = UUID.randomUUID().toString();
    private String hashedTokenValue;
    private User user;
    private UserSession session;
    private RefreshToken refreshToken;

    @BeforeAll
    void beforeAll(){
        transactionTemplate.setPropagationBehavior(TransactionTemplate.PROPAGATION_REQUIRES_NEW);

        encodedPassword = passwordEncoder.encode(password);
        hashedTokenValue = authUtils.hashTokenValue(tokenValue);
    }

    @BeforeEach
    void beforeEach(){
        user = userRepository.save(new User(null, "username", encodedPassword, User.UserRole.CUSTOMER));
        session = sessionRepository.save(new UserSession(null, true, now, now.plusSeconds(3600), user));
        refreshToken = refreshTokenRepository.save(new RefreshToken(null, hashedTokenValue, now, false, session, null));
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
                .when(authUtils).hashTokenValue(anyString());

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
                .when(authUtils).hashTokenValue(anyString());

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
                .when(authUtils).buildRefreshToken(anyString(), any(Instant.class), any(UserSession.class));

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
                .when(authUtils).hashTokenValue(anyString());

            //Act
            AccessTokenResponse response = authService.refreshAccess(request);

            //Assert
            assertThat(response).isNotNull();
            assertThat(sessionRepository.count()).isEqualTo(1);
            assertThat(refreshTokenRepository.count()).isEqualTo(2);
            assertThat(refreshTokenRepository.findById(refreshToken.getId()).orElseThrow().isUsed()).isTrue();
        }

    }

}
