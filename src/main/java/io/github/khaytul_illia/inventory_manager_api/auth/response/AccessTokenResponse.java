package io.github.khaytul_illia.inventory_manager_api.auth.response;

import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;

public record AccessTokenResponse(
    String issuer,
    Instant issuedAt,
    Instant expiresAt,
    String subject,
    String role,
    String accessToken,
    String refreshToken
) {

    public AccessTokenResponse(Jwt jwt, String refreshToken){
        this(
            jwt.getIssuer().toString(),
            jwt.getIssuedAt(),
            jwt.getExpiresAt(),
            jwt.getSubject(),
            jwt.getClaimAsString("roles"),
            jwt.getTokenValue(),
            refreshToken
        );
    }

}
