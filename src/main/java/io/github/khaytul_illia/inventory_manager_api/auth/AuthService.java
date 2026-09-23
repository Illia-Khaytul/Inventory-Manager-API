package io.github.khaytul_illia.inventory_manager_api.auth;

import io.github.khaytul_illia.inventory_manager_api.auth.request.LoginRequest;
import io.github.khaytul_illia.inventory_manager_api.auth.response.AccessTokenResponse;
import io.github.khaytul_illia.inventory_manager_api.error.exception.UserSessionLimitExceededException;
import io.github.khaytul_illia.inventory_manager_api.security.login.AppUserDetails;
import io.github.khaytul_illia.inventory_manager_api.user.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@Slf4j
public class AuthService {

    private final int maxOpenUserSessions;

    private final UserSessionRepository sessionRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final AuthenticationManager authenticationManager;
    private final AuthUtils authUtils;

    public AuthService(
        @Value("${spring.application.security.user_sessions.limit}")
        int maxOpenUserSessions,
        UserSessionRepository sessionRepository,
        RefreshTokenRepository refreshTokenRepository,
        AuthenticationManager authenticationManager,
        AuthUtils authUtils
    ) {
        this.maxOpenUserSessions = maxOpenUserSessions;

        this.sessionRepository = sessionRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.authenticationManager = authenticationManager;
        this.authUtils = authUtils;
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
            throw new BadCredentialsException("Invalid credentials", e);
        }
        AppUserDetails userDetails = (AppUserDetails) authentication.getPrincipal();
        User user = userDetails.getUser();

        log.debug("Checking if user has not reached maximum open session limit");

        if(sessionRepository.countOpenUserSessions(user.getId()) >= maxOpenUserSessions){
            throw new UserSessionLimitExceededException("Maximum amount of user sessions opened (%s)", maxOpenUserSessions);
        }

        log.debug("Opening new user session");

        Instant now = Instant.now();
        UserSession session = authUtils.buildUserSession(now, user);

        session = sessionRepository.save(session);

        log.debug("Generating new access and refresh tokens");

        String refreshTokenValue = UUID.randomUUID().toString();
        RefreshToken refreshToken = authUtils.buildRefreshToken(refreshTokenValue, now, session);

        refreshTokenRepository.save(refreshToken);

        Jwt jwt = authUtils.buildAccessToken(now, user);

        log.info("New session successfully opened with id {}", session.getId());

        return new AccessTokenResponse(jwt, refreshTokenValue);
    }

}
