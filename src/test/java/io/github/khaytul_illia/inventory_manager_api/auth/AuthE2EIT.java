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
import io.github.khaytul_illia.inventory_manager_api.error.ErrorResponse;
import io.github.khaytul_illia.inventory_manager_api.user.User;
import io.github.khaytul_illia.inventory_manager_api.user.UserRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.client.RestTestClient;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
@AutoConfigureRestTestClient
@ActiveProfiles("test")
@DisplayName("Authentication API end-to-end tests")
public class AuthE2EIT {

    @Autowired
    private RestTestClient restClient;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UserSessionRepository sessionRepository;
    @Autowired
    private RefreshTokenRepository refreshTokenRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private TokenFactory tokenFactory;

    private final String username = "username";
    private final String password = "password";

    @BeforeEach
    void beforeEach(){
        userRepository.save(new User(null, username, passwordEncoder.encode(password), User.UserRole.CUSTOMER));
    }

    @AfterEach
    void afterEach(){
        refreshTokenRepository.deleteAll();
        sessionRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("User logs in and opens a session, refreshes access to the session, then logs out and closes it")
    void userLogin_refreshAccess_logout_sessionGetsClosed(){
        //Login
        LoginRequest loginRequest = new LoginRequest(username, password);

        AccessTokenResponse loginTokenResponse = performSuccessfulLogin(loginRequest);
        String loginAccessToken = loginTokenResponse.accessToken();
        String loginRefreshToken = loginTokenResponse.refreshToken();

        assertThat(sessionRepository.count()).isEqualTo(1);
        assertThat(refreshTokenRepository.count()).isEqualTo(1);

        //Refresh access
        RefreshTokenRequest refreshAccessRequest = new RefreshTokenRequest(loginRefreshToken);

        AccessTokenResponse refreshTokenResponse = performSuccessfulRefreshAccess(refreshAccessRequest, loginAccessToken);
        String refreshAccessToken = refreshTokenResponse.accessToken();
        String refreshRefreshToken = refreshTokenResponse.refreshToken();

        String hashedLoginRefreshToken = tokenFactory.hashTokenValue(loginRefreshToken);
        RefreshToken loginRefreshTokenEntry = refreshTokenRepository.findByTokenValue(hashedLoginRefreshToken).orElseThrow();
        UserSession session = loginRefreshTokenEntry.getSession();

        assertThat(refreshTokenRepository.count()).isEqualTo(2);
        assertThat(loginRefreshTokenEntry.isUsed()).isTrue();

        //Logout
        RefreshTokenRequest logoutRequest = new RefreshTokenRequest(refreshRefreshToken);

        performLogout(logoutRequest, refreshAccessToken);

        assertThat(refreshTokenRepository.count()).isEqualTo(2);
        assertThat(sessionRepository.findById(session.getId()).orElseThrow().isValid()).isFalse();
    }

    @Test
    @DisplayName("User logs in and opens a session, refreshes access first with a valid token, then with a used one, the session gets closed automatically")
    void userLogin_refreshAccess_refreshAccessWithUsedToken_sessionGetsClosed(){
        //Login
        LoginRequest loginRequest = new LoginRequest(username, password);

        AccessTokenResponse loginTokenResponse = performSuccessfulLogin(loginRequest);
        String loginAccessToken = loginTokenResponse.accessToken();
        String loginRefreshToken = loginTokenResponse.refreshToken();

        assertThat(sessionRepository.count()).isEqualTo(1);
        assertThat(refreshTokenRepository.count()).isEqualTo(1);

        //Refresh access
        RefreshTokenRequest refreshAccessRequest = new RefreshTokenRequest(loginRefreshToken);

        performSuccessfulRefreshAccess(refreshAccessRequest, loginAccessToken);

        String hashedLoginRefreshToken = tokenFactory.hashTokenValue(loginRefreshToken);
        RefreshToken loginRefreshTokenEntry = refreshTokenRepository.findByTokenValue(hashedLoginRefreshToken).orElseThrow();
        UserSession session = loginRefreshTokenEntry.getSession();

        assertThat(refreshTokenRepository.count()).isEqualTo(2);
        assertThat(loginRefreshTokenEntry.isUsed()).isTrue();
        assertThat(session.isValid()).isTrue();

        //Refresh access with used token
        performErrorRefreshAccess(refreshAccessRequest, HttpStatus.UNAUTHORIZED, "Refresh token is used or session is invalid or expired");

        assertThat(refreshTokenRepository.count()).isEqualTo(2);
        assertThat(sessionRepository.findById(session.getId()).orElseThrow().isValid()).isFalse();
    }

    @Test
    @DisplayName("User logs in twice and opens two sessions, then logs out of the first one, the second one remains open")
    void userLogin_loginAgain_logoutFirstSession_secondSessionRemainsOpen(){
        //First login
        LoginRequest loginRequest = new LoginRequest(username, password);

        AccessTokenResponse firstLoginResponse = performSuccessfulLogin(loginRequest);
        String firstAccessToken = firstLoginResponse.accessToken();
        String firstSessionToken = firstLoginResponse.refreshToken();

        assertThat(sessionRepository.count()).isEqualTo(1);
        assertThat(refreshTokenRepository.count()).isEqualTo(1);

        //Second login
        AccessTokenResponse secondLoginResponse = performSuccessfulLogin(loginRequest);
        String secondAccessToken = secondLoginResponse.accessToken();
        String secondSessionToken = secondLoginResponse.refreshToken();

        assertThat(firstAccessToken).isNotEqualTo(secondAccessToken);
        assertThat(firstSessionToken).isNotEqualTo(secondSessionToken);

        assertThat(sessionRepository.count()).isEqualTo(2);
        assertThat(refreshTokenRepository.count()).isEqualTo(2);

        //Logout first session
        RefreshTokenRequest logoutRequest = new RefreshTokenRequest(firstSessionToken);

        performLogout(logoutRequest, firstAccessToken);

        String hashedFirstSessionToken = tokenFactory.hashTokenValue(firstSessionToken);
        String hashedSecondSessionToken = tokenFactory.hashTokenValue(secondSessionToken);

        assertThat(sessionRepository.count()).isEqualTo(2);
        assertThat(refreshTokenRepository.findByTokenValue(hashedFirstSessionToken).orElseThrow().getSession().isValid()).isFalse();
        assertThat(refreshTokenRepository.findByTokenValue(hashedSecondSessionToken).orElseThrow().getSession().isValid()).isTrue();
    }

    @Test
    @DisplayName("User logs in twice and opens two sessions, then logs out of all sessions, all sessions get closed")
    void userLogin_loginAgain_logoutAll_allSessionsGetClosed(){
        //First login
        LoginRequest loginRequest = new LoginRequest(username, password);

        AccessTokenResponse firstLoginResponse = performSuccessfulLogin(loginRequest);
        String firstAccessToken = firstLoginResponse.accessToken();
        String firstSessionToken = firstLoginResponse.refreshToken();

        assertThat(sessionRepository.count()).isEqualTo(1);
        assertThat(refreshTokenRepository.count()).isEqualTo(1);

        //Second login
        AccessTokenResponse secondLoginResponse = performSuccessfulLogin(loginRequest);
        String secondAccessToken = secondLoginResponse.accessToken();
        String secondSessionToken = secondLoginResponse.refreshToken();

        assertThat(firstAccessToken).isNotEqualTo(secondAccessToken);
        assertThat(firstSessionToken).isNotEqualTo(secondSessionToken);

        assertThat(sessionRepository.count()).isEqualTo(2);
        assertThat(refreshTokenRepository.count()).isEqualTo(2);

        //Logout all
        performLogoutAll(firstAccessToken);

        String hashedFirstSessionToken = tokenFactory.hashTokenValue(firstSessionToken);
        String hashedSecondSessionToken = tokenFactory.hashTokenValue(secondSessionToken);

        assertThat(sessionRepository.count()).isEqualTo(2);
        assertThat(refreshTokenRepository.findByTokenValue(hashedFirstSessionToken).orElseThrow().getSession().isValid()).isFalse();
        assertThat(refreshTokenRepository.findByTokenValue(hashedSecondSessionToken).orElseThrow().getSession().isValid()).isFalse();
    }

    @Test
    @DisplayName("User logs in and opens a session, logs out and closes it, then tries to refresh access to that session, nothing happens, session already closed")
    void userLogin_logout_refreshAccess_nothingHappens(){
        //Login
        LoginRequest loginRequest = new LoginRequest(username, password);

        AccessTokenResponse loginTokenResponse = performSuccessfulLogin(loginRequest);
        String loginAccessToken = loginTokenResponse.accessToken();
        String loginRefreshToken = loginTokenResponse.refreshToken();

        assertThat(sessionRepository.count()).isEqualTo(1);
        assertThat(refreshTokenRepository.count()).isEqualTo(1);

        //Logout
        RefreshTokenRequest logoutRequest = new RefreshTokenRequest(loginRefreshToken);

        performLogout(logoutRequest, loginAccessToken);

        String hashedLoginRefreshToken = tokenFactory.hashTokenValue(loginRefreshToken);
        RefreshToken loginRefreshTokenEntry = refreshTokenRepository.findByTokenValue(hashedLoginRefreshToken).orElseThrow();
        UserSession session = loginRefreshTokenEntry.getSession();

        assertThat(loginRefreshTokenEntry.isUsed()).isFalse();
        assertThat(session.isValid()).isFalse();

        //Refresh access
        RefreshTokenRequest refreshAccessRequest = new RefreshTokenRequest(loginRefreshToken);

        performErrorRefreshAccess(refreshAccessRequest, HttpStatus.UNAUTHORIZED, "Refresh token is used or session is invalid or expired");

        assertThat(refreshTokenRepository.count()).isEqualTo(1);
        assertThat(refreshTokenRepository.findById(loginRefreshTokenEntry.getId()).orElseThrow().isUsed()).isFalse();
        assertThat(sessionRepository.findById(session.getId()).orElseThrow().isValid()).isFalse();
    }

    @Test
    @DisplayName("User logs in and opens a session, logs out and closes it, then logs out for the same session again, nothing happens, session already closed")
    void userLogin_logout_logoutAgain_nothingHappens(){
        //Login
        LoginRequest loginRequest = new LoginRequest(username, password);

        AccessTokenResponse loginTokenResponse = performSuccessfulLogin(loginRequest);
        String loginAccessToken = loginTokenResponse.accessToken();
        String loginRefreshToken = loginTokenResponse.refreshToken();

        assertThat(sessionRepository.count()).isEqualTo(1);
        assertThat(refreshTokenRepository.count()).isEqualTo(1);

        //Logout
        RefreshTokenRequest logoutRequest = new RefreshTokenRequest(loginRefreshToken);

        performLogout(logoutRequest, loginAccessToken);

        String hashedLoginRefreshToken = tokenFactory.hashTokenValue(loginRefreshToken);
        RefreshToken loginRefreshTokenEntry = refreshTokenRepository.findByTokenValue(hashedLoginRefreshToken).orElseThrow();
        UserSession session = loginRefreshTokenEntry.getSession();

        assertThat(loginRefreshTokenEntry.isUsed()).isFalse();
        assertThat(session.isValid()).isFalse();

        //Logout same session
        performLogout(logoutRequest, loginAccessToken);

        assertThat(refreshTokenRepository.findById(loginRefreshTokenEntry.getId()).orElseThrow().isUsed()).isFalse();
        assertThat(sessionRepository.findById(session.getId()).orElseThrow().isValid()).isFalse();
    }

    /*
            Helper methods
     */

    private AccessTokenResponse performSuccessfulLogin(LoginRequest request){
        AccessTokenResponse response = restClient
            .post()
            .uri("/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .body(objectMapper.writeValueAsString(request))
            .exchange()
            .expectStatus().isOk()
            .returnResult(AccessTokenResponse.class).getResponseBody();

        assertThat(response).isNotNull();
        assertThat(response.subject()).isEqualTo(username);
        assertThat(response.accessToken()).isNotBlank();
        assertThat(response.refreshToken()).isNotBlank();

        return response;
    }

    private AccessTokenResponse performSuccessfulRefreshAccess(RefreshTokenRequest request, String accessToken){
        AccessTokenResponse response = restClient
            .post()
            .uri("/auth/refresh-access")
            .contentType(MediaType.APPLICATION_JSON)
            .body(objectMapper.writeValueAsString(request))
            .exchange()
            .expectStatus().isOk()
            .returnResult(AccessTokenResponse.class).getResponseBody();

        assertThat(response).isNotNull();
        assertThat(response.subject()).isEqualTo(username);

        String refreshAccessToken = response.accessToken();
        String refreshRefreshToken = response.refreshToken();

        assertThat(refreshAccessToken).isNotBlank();
        assertThat(refreshRefreshToken).isNotBlank();
        assertThat(refreshAccessToken).isNotEqualTo(accessToken);
        assertThat(refreshRefreshToken).isNotEqualTo(request.refreshToken());

        return response;
    }

    private void performErrorRefreshAccess(RefreshTokenRequest request, HttpStatus status, String errorMessage){
        ErrorResponse errorResponse = restClient
            .post()
            .uri("/auth/refresh-access")
            .contentType(MediaType.APPLICATION_JSON)
            .body(objectMapper.writeValueAsString(request))
            .exchange()
            .expectStatus().isEqualTo(status.value())
            .returnResult(ErrorResponse.class).getResponseBody();

        assertThat(errorResponse).isNotNull();
        assertThat(errorResponse.status()).isEqualTo(status.value());
        assertThat(errorResponse.message()).isEqualTo(errorMessage);
    }

    private void performLogout(RefreshTokenRequest request, String accessToken){
        restClient
            .post()
            .uri("/auth/logout")
            .header("Authorization", "Bearer " + accessToken)
            .contentType(MediaType.APPLICATION_JSON)
            .body(objectMapper.writeValueAsString(request))
            .exchange()
            .expectStatus().isNoContent()
            .expectBody(Void.class);
    }

    private void performLogoutAll(String accessToken){
        restClient
            .post()
            .uri("/auth/logout-all")
            .header("Authorization", "Bearer " + accessToken)
            .exchange()
            .expectStatus().isNoContent()
            .expectBody(Void.class);
    }

}
