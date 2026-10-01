package io.github.khaytul_illia.inventory_manager_api.auth;

import io.github.khaytul_illia.inventory_manager_api.auth.token.RefreshToken;
import io.github.khaytul_illia.inventory_manager_api.auth.request.LoginRequest;
import io.github.khaytul_illia.inventory_manager_api.auth.request.RefreshTokenRequest;
import io.github.khaytul_illia.inventory_manager_api.auth.response.AccessTokenResponse;
import io.github.khaytul_illia.inventory_manager_api.auth.session.UserSession;
import io.github.khaytul_illia.inventory_manager_api.auth.session.UserSessionService;
import io.github.khaytul_illia.inventory_manager_api.auth.token.TokenService;
import io.github.khaytul_illia.inventory_manager_api.error.exception.FailedLoginAuthenticationException;
import io.github.khaytul_illia.inventory_manager_api.error.exception.InvalidRefreshTokenException;
import io.github.khaytul_illia.inventory_manager_api.security.SecurityUtils;
import io.github.khaytul_illia.inventory_manager_api.security.login.AppUserDetails;
import io.github.khaytul_illia.inventory_manager_api.user.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@Slf4j
public class AuthService {

    private final UserSessionService sessionService;
    private final TokenService tokenService;
    private final AuthenticationManager authenticationManager;
    private final SecurityUtils securityUtils;

    public AuthService(
        UserSessionService sessionService,
        TokenService tokenService,
        AuthenticationManager authenticationManager,
        SecurityUtils securityUtils
    ) {
        this.sessionService = sessionService;
        this.tokenService = tokenService;
        this.authenticationManager = authenticationManager;
        this.securityUtils = securityUtils;
    }

    @Transactional
    public AccessTokenResponse login(LoginRequest request){
        log.info("New login attempt for user '{}'", request.username());

        log.debug("Authenticating user with credentials");

        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password())
            );
        }catch(AuthenticationException e){
            throw new FailedLoginAuthenticationException(e);
        }
        AppUserDetails userDetails = (AppUserDetails) authentication.getPrincipal();
        User user = userDetails.getUser();

        UserSession session = sessionService.createUserSession(user.getId());
        AccessTokenResponse accessTokenResponse = tokenService.createAccessRefreshTokenPair(session);

        log.info("New session successfully opened with id {}", session.getId());

        return accessTokenResponse;
    }

    @Transactional
    public AccessTokenResponse refreshAccess(RefreshTokenRequest request){
        log.info("Access refresh attempt");

        RefreshToken refreshToken = tokenService.useRefreshToken(request.refreshToken());
        UserSession session = refreshToken.getSession();

        AccessTokenResponse accessTokenResponse = tokenService.createAccessRefreshTokenPair(session);

        log.info("Access for session with id {} successfully refreshed", session.getId());

        return accessTokenResponse;
    }

    public void logout(RefreshTokenRequest request){
        log.info("Logout from session");

        RefreshToken refreshToken;
        try {
            refreshToken = tokenService.loadRefreshToken(request.refreshToken());
        }catch(InvalidRefreshTokenException e){
            return;
        }
        UserSession session = refreshToken.getSession();
        User user = session.getUser();

        log.debug("Checking if the found session belongs to the authenticated user");

        String authenticatedUserUsername = securityUtils.getAuthenticatedUserAccessToken().getSubject();
        if(!user.getUsername().equals(authenticatedUserUsername)){
            return;
        }

        sessionService.invalidateSession(session.getId());

        log.info("Successfully logged out from session with id {}", session.getId());
    }

    public void logoutAll(){
        log.info("Logout from all sessions");

        String username = securityUtils.getAuthenticatedUserAccessToken().getSubject();

        sessionService.invalidateAllUserSessions(username);

        log.info("Successfully logged out from all sessions for user '{}'", username);
    }

}
