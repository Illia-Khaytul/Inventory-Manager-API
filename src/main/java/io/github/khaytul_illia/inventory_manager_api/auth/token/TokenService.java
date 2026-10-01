package io.github.khaytul_illia.inventory_manager_api.auth.token;

import io.github.khaytul_illia.inventory_manager_api.auth.response.AccessTokenResponse;
import io.github.khaytul_illia.inventory_manager_api.auth.session.UserSession;
import io.github.khaytul_illia.inventory_manager_api.auth.session.UserSessionService;
import io.github.khaytul_illia.inventory_manager_api.error.exception.InvalidRefreshTokenException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class TokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserSessionService sessionService;
    private final TokenFactory tokenFactory;

    public TokenService(
        RefreshTokenRepository refreshTokenRepository,
        UserSessionService sessionService,
        TokenFactory tokenFactory
    ) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.sessionService = sessionService;
        this.tokenFactory = tokenFactory;
    }

    @Transactional
    public AccessTokenResponse createAccessRefreshTokenPair(UserSession session){
        log.debug("Generating new access and refresh tokens");

        Instant now = Instant.now();
        String refreshTokenValue = UUID.randomUUID().toString();

        RefreshToken refreshToken = tokenFactory.buildRefreshToken(refreshTokenValue, now, session);

        refreshTokenRepository.save(refreshToken);

        Jwt accessToken = tokenFactory.buildAccessToken(now, session.getUser());

        return new AccessTokenResponse(accessToken, refreshTokenValue);
    }

    @Transactional
    public RefreshToken useRefreshToken(String tokenValue){
        RefreshToken refreshToken = loadRefreshToken(tokenValue);
        UserSession session = refreshToken.getSession();
        Instant now = Instant.now();

        log.debug("Checking if the refresh token is not used, and the session is valid and not expired");

        boolean isTokenUsed = refreshToken.isUsed();
        boolean isSessionInvalid = !session.isValid();
        boolean isSessionExpired = session.getExpiresAt().isBefore(now);
        if(isTokenUsed || isSessionInvalid || isSessionExpired){
            if(!isSessionInvalid) {
                sessionService.invalidateSession(session.getId());
            }

            List<String> details = new ArrayList<>();
            if(isTokenUsed) details.add("Detected refresh token reuse");
            if(isSessionInvalid) details.add("Attempted access to an invalidated session");
            if(isSessionExpired) details.add("Attempted access to an expired session");

            throw new InvalidRefreshTokenException("Refresh token is used or session is invalid or expired", details);
        }

        log.debug("Using previous refresh token");

        refreshToken.setUsed(true);

        return refreshToken;
    }

    @Transactional(readOnly = true)
    public RefreshToken loadRefreshToken(String tokenValue){
        log.debug("Loading the refresh token by value");

        String refreshTokenValue = tokenFactory.hashTokenValue(tokenValue);

        return refreshTokenRepository.findByTokenValue(refreshTokenValue)
            .orElseThrow(() -> new InvalidRefreshTokenException("Refresh token is used or session is invalid or expired", List.of("Refresh token does not exist")));
    }

}
