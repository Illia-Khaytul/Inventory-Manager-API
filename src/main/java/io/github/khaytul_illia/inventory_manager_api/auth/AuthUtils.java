package io.github.khaytul_illia.inventory_manager_api.auth;

import io.github.khaytul_illia.inventory_manager_api.user.User;
import io.github.khaytul_illia.inventory_manager_api.user.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Component;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;

@Component
public class AuthUtils {

    private final int userSessionLifetime;
    private final int accessTokenLifetime;
    private final String accessTokenIssuer;

    private final UserRepository userRepository;
    private final JwtEncoder jwtEncoder;

    public AuthUtils(
        @Value("${spring.application.security.user_sessions.lifetime}")
        int userSessionLifetime,
        @Value("${spring.application.security.jwt.lifetime}")
        int accessTokenLifetime,
        @Value("${spring.application.security.jwt.issuer}")
        String accessTokenIssuer,
        UserRepository userRepository,
        JwtEncoder jwtEncoder
    ) {
        this.userSessionLifetime = userSessionLifetime;
        this.accessTokenLifetime = accessTokenLifetime;
        this.accessTokenIssuer = accessTokenIssuer;

        this.userRepository = userRepository;
        this.jwtEncoder = jwtEncoder;
    }

    public UserSession buildUserSession(Instant createdAt, User user){
        UserSession session = new UserSession();
        session.setValid(true);
        session.setCreatedAt(createdAt);
        session.setExpiresAt(createdAt.plusSeconds(userSessionLifetime));
        session.setUser(userRepository.getReferenceById(user.getId()));

        return session;
    }

    public RefreshToken buildRefreshToken(String tokenValue, Instant issuedAt, UserSession session){
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setTokenValue(hashTokenValue(tokenValue));
        refreshToken.setIssuedAt(issuedAt);
        refreshToken.setUsed(false);
        refreshToken.setSession(session);

        return refreshToken;
    }

    public Jwt buildAccessToken(Instant issuedAt, User user){
        JwtClaimsSet jwtClaims = JwtClaimsSet.builder()
            .issuer(accessTokenIssuer)
            .issuedAt(issuedAt)
            .expiresAt(issuedAt.plusSeconds(accessTokenLifetime))
            .subject(user.getUsername())
            .claim("roles", user.getRole().name())
            .build();
        JwsHeader jwtHeader = JwsHeader.with(SignatureAlgorithm.RS512).build();
        JwtEncoderParameters jwtParameters = JwtEncoderParameters.from(jwtHeader, jwtClaims);

        return jwtEncoder.encode(jwtParameters);
    }

    public String hashTokenValue(String tokenValue){
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = messageDigest.digest(tokenValue.getBytes());

            StringBuilder hexString = new StringBuilder(2 * hashBytes.length);
            for (byte hashByte: hashBytes) {
                String hex = Integer.toHexString(0xff & hashByte);
                if(hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }

            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("No such algorithm available", e);
        }
    }

}
