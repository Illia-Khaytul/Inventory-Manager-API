package io.github.khaytul_illia.inventory_manager_api.auth;

import io.github.khaytul_illia.inventory_manager_api.auth.token.TokenFactory;
import io.github.khaytul_illia.inventory_manager_api.support.context.E2ETestContext;
import io.github.khaytul_illia.inventory_manager_api.auth.token.RefreshToken;
import io.github.khaytul_illia.inventory_manager_api.auth.request.LoginRequest;
import io.github.khaytul_illia.inventory_manager_api.auth.request.RefreshTokenRequest;
import io.github.khaytul_illia.inventory_manager_api.auth.response.AccessTokenResponse;
import io.github.khaytul_illia.inventory_manager_api.auth.session.UserSession;
import io.github.khaytul_illia.inventory_manager_api.support.utils.e2e.AuthRequestUtils;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Authentication API end-to-end tests")
public class AuthE2EIT extends E2ETestContext {

    @Autowired
    private TokenFactory tokenFactory;

    @Test
    @DisplayName("User logs in and opens a session, refreshes access to the session, then logs out and closes it")
    void userLogin_refreshAccess_logout_sessionGetsClosed(){
        //Login
        LoginRequest loginRequest = new LoginRequest(username, password);

        AccessTokenResponse loginTokenResponse = authRequestUtils.performSuccessfulLogin(loginRequest);
        String loginAccessToken = loginTokenResponse.accessToken();
        String loginRefreshToken = loginTokenResponse.refreshToken();

        assertThat(sessionRepository.count()).isEqualTo(1);
        assertThat(refreshTokenRepository.count()).isEqualTo(1);

        //Refresh access
        RefreshTokenRequest refreshAccessRequest = new RefreshTokenRequest(loginRefreshToken);

        AccessTokenResponse refreshTokenResponse = authRequestUtils.performSuccessfulRefreshAccess(refreshAccessRequest, username, loginAccessToken);
        String refreshAccessToken = refreshTokenResponse.accessToken();
        String refreshRefreshToken = refreshTokenResponse.refreshToken();

        String hashedLoginRefreshToken = tokenFactory.hashTokenValue(loginRefreshToken);
        RefreshToken loginRefreshTokenEntry = refreshTokenRepository.findByTokenValue(hashedLoginRefreshToken).orElseThrow();
        UserSession session = loginRefreshTokenEntry.getSession();

        assertThat(refreshTokenRepository.count()).isEqualTo(2);
        assertThat(loginRefreshTokenEntry.isUsed()).isTrue();

        //Logout
        RefreshTokenRequest logoutRequest = new RefreshTokenRequest(refreshRefreshToken);

        authRequestUtils.performLogout(logoutRequest, refreshAccessToken);

        assertThat(refreshTokenRepository.count()).isEqualTo(2);
        assertThat(sessionRepository.findById(session.getId()).orElseThrow().isValid()).isFalse();
    }

    @Test
    @DisplayName("User logs in and opens a session, refreshes access first with a valid token, then with a used one, the session gets closed automatically")
    void userLogin_refreshAccess_refreshAccessWithUsedToken_sessionGetsClosed(){
        //Login
        LoginRequest loginRequest = new LoginRequest(username, password);

        AccessTokenResponse loginTokenResponse = authRequestUtils.performSuccessfulLogin(loginRequest);
        String loginAccessToken = loginTokenResponse.accessToken();
        String loginRefreshToken = loginTokenResponse.refreshToken();

        assertThat(sessionRepository.count()).isEqualTo(1);
        assertThat(refreshTokenRepository.count()).isEqualTo(1);

        //Refresh access
        RefreshTokenRequest refreshAccessRequest = new RefreshTokenRequest(loginRefreshToken);

        authRequestUtils.performSuccessfulRefreshAccess(refreshAccessRequest, username, loginAccessToken);

        String hashedLoginRefreshToken = tokenFactory.hashTokenValue(loginRefreshToken);
        RefreshToken loginRefreshTokenEntry = refreshTokenRepository.findByTokenValue(hashedLoginRefreshToken).orElseThrow();
        UserSession session = loginRefreshTokenEntry.getSession();

        assertThat(refreshTokenRepository.count()).isEqualTo(2);
        assertThat(loginRefreshTokenEntry.isUsed()).isTrue();
        assertThat(session.isValid()).isTrue();

        //Refresh access with used token
        authRequestUtils.performErrorRefreshAccess(refreshAccessRequest, HttpStatus.UNAUTHORIZED, "Refresh token is used or session is invalid or expired");

        assertThat(refreshTokenRepository.count()).isEqualTo(2);
        assertThat(sessionRepository.findById(session.getId()).orElseThrow().isValid()).isFalse();
    }

    @Test
    @DisplayName("User logs in twice and opens two sessions, then logs out of the first one, the second one remains open")
    void userLogin_loginAgain_logoutFirstSession_secondSessionRemainsOpen(){
        //First login
        LoginRequest loginRequest = new LoginRequest(username, password);

        AccessTokenResponse firstLoginResponse = authRequestUtils.performSuccessfulLogin(loginRequest);
        String firstAccessToken = firstLoginResponse.accessToken();
        String firstSessionToken = firstLoginResponse.refreshToken();

        assertThat(sessionRepository.count()).isEqualTo(1);
        assertThat(refreshTokenRepository.count()).isEqualTo(1);

        //Second login
        AccessTokenResponse secondLoginResponse = authRequestUtils.performSuccessfulLogin(loginRequest);
        String secondAccessToken = secondLoginResponse.accessToken();
        String secondSessionToken = secondLoginResponse.refreshToken();

        assertThat(firstAccessToken).isNotEqualTo(secondAccessToken);
        assertThat(firstSessionToken).isNotEqualTo(secondSessionToken);

        assertThat(sessionRepository.count()).isEqualTo(2);
        assertThat(refreshTokenRepository.count()).isEqualTo(2);

        //Logout first session
        RefreshTokenRequest logoutRequest = new RefreshTokenRequest(firstSessionToken);

        authRequestUtils.performLogout(logoutRequest, firstAccessToken);

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

        AccessTokenResponse firstLoginResponse = authRequestUtils.performSuccessfulLogin(loginRequest);
        String firstAccessToken = firstLoginResponse.accessToken();
        String firstSessionToken = firstLoginResponse.refreshToken();

        assertThat(sessionRepository.count()).isEqualTo(1);
        assertThat(refreshTokenRepository.count()).isEqualTo(1);

        //Second login
        AccessTokenResponse secondLoginResponse = authRequestUtils.performSuccessfulLogin(loginRequest);
        String secondAccessToken = secondLoginResponse.accessToken();
        String secondSessionToken = secondLoginResponse.refreshToken();

        assertThat(firstAccessToken).isNotEqualTo(secondAccessToken);
        assertThat(firstSessionToken).isNotEqualTo(secondSessionToken);

        assertThat(sessionRepository.count()).isEqualTo(2);
        assertThat(refreshTokenRepository.count()).isEqualTo(2);

        //Logout all
        authRequestUtils.performLogoutAll(firstAccessToken);

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

        AccessTokenResponse loginTokenResponse = authRequestUtils.performSuccessfulLogin(loginRequest);
        String loginAccessToken = loginTokenResponse.accessToken();
        String loginRefreshToken = loginTokenResponse.refreshToken();

        assertThat(sessionRepository.count()).isEqualTo(1);
        assertThat(refreshTokenRepository.count()).isEqualTo(1);

        //Logout
        RefreshTokenRequest logoutRequest = new RefreshTokenRequest(loginRefreshToken);

        authRequestUtils.performLogout(logoutRequest, loginAccessToken);

        String hashedLoginRefreshToken = tokenFactory.hashTokenValue(loginRefreshToken);
        RefreshToken loginRefreshTokenEntry = refreshTokenRepository.findByTokenValue(hashedLoginRefreshToken).orElseThrow();
        UserSession session = loginRefreshTokenEntry.getSession();

        assertThat(loginRefreshTokenEntry.isUsed()).isFalse();
        assertThat(session.isValid()).isFalse();

        //Refresh access
        RefreshTokenRequest refreshAccessRequest = new RefreshTokenRequest(loginRefreshToken);

        authRequestUtils.performErrorRefreshAccess(refreshAccessRequest, HttpStatus.UNAUTHORIZED, "Refresh token is used or session is invalid or expired");

        assertThat(refreshTokenRepository.count()).isEqualTo(1);
        assertThat(refreshTokenRepository.findById(loginRefreshTokenEntry.getId()).orElseThrow().isUsed()).isFalse();
        assertThat(sessionRepository.findById(session.getId()).orElseThrow().isValid()).isFalse();
    }

    @Test
    @DisplayName("User logs in and opens a session, logs out and closes it, then logs out for the same session again, nothing happens, session already closed")
    void userLogin_logout_logoutAgain_nothingHappens(){
        //Login
        LoginRequest loginRequest = new LoginRequest(username, password);

        AccessTokenResponse loginTokenResponse = authRequestUtils.performSuccessfulLogin(loginRequest);
        String loginAccessToken = loginTokenResponse.accessToken();
        String loginRefreshToken = loginTokenResponse.refreshToken();

        assertThat(sessionRepository.count()).isEqualTo(1);
        assertThat(refreshTokenRepository.count()).isEqualTo(1);

        //Logout
        RefreshTokenRequest logoutRequest = new RefreshTokenRequest(loginRefreshToken);

        authRequestUtils.performLogout(logoutRequest, loginAccessToken);

        String hashedLoginRefreshToken = tokenFactory.hashTokenValue(loginRefreshToken);
        RefreshToken loginRefreshTokenEntry = refreshTokenRepository.findByTokenValue(hashedLoginRefreshToken).orElseThrow();
        UserSession session = loginRefreshTokenEntry.getSession();

        assertThat(loginRefreshTokenEntry.isUsed()).isFalse();
        assertThat(session.isValid()).isFalse();

        //Logout same session
        authRequestUtils.performLogout(logoutRequest, loginAccessToken);

        assertThat(refreshTokenRepository.findById(loginRefreshTokenEntry.getId()).orElseThrow().isUsed()).isFalse();
        assertThat(sessionRepository.findById(session.getId()).orElseThrow().isValid()).isFalse();
    }

}
