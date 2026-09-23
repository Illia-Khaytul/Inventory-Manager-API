package io.github.khaytul_illia.inventory_manager_api.auth;

import io.github.khaytul_illia.inventory_manager_api.user.User;
import io.github.khaytul_illia.inventory_manager_api.user.UserRepository;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.testcontainers.shaded.org.bouncycastle.jcajce.BCFKSLoadStoreParameter;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthUtils tests")
public class AuthUtilsTests {

    private int userSessionLifetime = 3600;
    private int accessTokenLifetime = 900;
    private String accessTokenIssuer = "http://localhost:8080/api/v1";
    @Mock
    private UserRepository userRepository;
    @Mock
    private JwtEncoder jwtEncoder;
    private AuthUtils authUtils;

    @BeforeEach
    void beforeEach(){
        authUtils = new AuthUtils(
            userSessionLifetime,
            accessTokenLifetime,
            accessTokenIssuer,
            userRepository,
            jwtEncoder
        );
    }

    @Test
    @DisplayName("Should build new UserSession entity")
    void shouldBuildUserSession(){
        //Arrange
        Instant createdAt = Instant.now();
        User user = new User(1L, "username", "password", User.UserRole.CUSTOMER);

        when(userRepository.getReferenceById(user.getId()))
            .thenReturn(user);

        //Act
        UserSession session = authUtils.buildUserSession(createdAt, user);

        //Assert
        assertThat(session).isNotNull();
        assertThat(session.getId()).isNull();
        assertThat(session.getCreatedAt()).isEqualTo(createdAt);
        assertThat(session.getExpiresAt()).isEqualTo(createdAt.plusSeconds(userSessionLifetime).toString());
        assertThat(session.isValid()).isTrue();
        assertThat(session.getUser().getId()).isEqualTo(user.getId());

        verify(userRepository).getReferenceById(user.getId());
    }

    @Test
    @DisplayName("Should build RefreshToken entity")
    void shouldBuildRefreshToken(){
        //Arrange
        String tokenValue = "refresh token value";
        String tokenHash = "token hash";
        Instant issuedAt = Instant.now();
        User user = new User(1L, "username", "password", User.UserRole.CUSTOMER);
        UserSession session = new UserSession(1L, true, issuedAt, issuedAt.plusSeconds(userSessionLifetime), user);

        AuthUtils spyAuthUtils = spy(authUtils);
        when(spyAuthUtils.hashTokenValue(tokenValue))
            .thenReturn(tokenHash);

        //Act
        RefreshToken refreshToken = spyAuthUtils.buildRefreshToken(tokenValue, issuedAt, session);

        //Assert
        assertThat(refreshToken).isNotNull();
        assertThat(refreshToken.getId()).isNull();
        assertThat(refreshToken.getTokenValue()).isEqualTo(tokenHash);
        assertThat(refreshToken.getIssuedAt()).isEqualTo(issuedAt);
        assertThat(refreshToken.isUsed()).isFalse();
        assertThat(refreshToken.getSession().getId()).isEqualTo(session.getId());
        assertThat(refreshToken.getVersion()).isNull();

        verify(spyAuthUtils).hashTokenValue(tokenValue);
    }

    @Test
    @DisplayName("Should build JWT access token")
    void shouldBuildJWT(){
        //Arrange
        Instant issuedAt = Instant.now();
        User user = new User(1L, "username", "password", User.UserRole.CUSTOMER);
        Jwt generatedJwt = mock(Jwt.class);

        when(jwtEncoder.encode(any(JwtEncoderParameters.class)))
            .thenReturn(generatedJwt);

        //Act
        Jwt jwt = authUtils.buildAccessToken(issuedAt, user);

        //Assert
        assertThat(jwt).isNotNull();
        assertThat(jwt).isEqualTo(generatedJwt);

        ArgumentCaptor<JwtEncoderParameters> paramsCaptor = ArgumentCaptor.forClass(JwtEncoderParameters.class);
        verify(jwtEncoder).encode(paramsCaptor.capture());

        JwtEncoderParameters jwtParameters = paramsCaptor.getValue();

        JwsHeader jwtHeader = jwtParameters.getJwsHeader();
        assertThat(jwtHeader.getAlgorithm()).isEqualTo(SignatureAlgorithm.RS512);

        JwtClaimsSet jwtClaims = jwtParameters.getClaims();
        assertThat(jwtClaims.getIssuer().toString()).isEqualTo(accessTokenIssuer);
        assertThat(jwtClaims.getIssuedAt()).isEqualTo(issuedAt);
        assertThat(jwtClaims.getExpiresAt()).isEqualTo(issuedAt.plusSeconds(accessTokenLifetime).toString());
        assertThat(jwtClaims.getSubject()).isEqualTo(user.getUsername());
        assertThat(jwtClaims.getClaimAsString("roles")).isEqualTo(user.getRole().name());
    }

    @Test
    @DisplayName("Should return hash of the provided token value")
    void shouldHashTokenValue(){
        //Arrange
        String tokenValue = "token value";

        //Act
        String tokenHash = authUtils.hashTokenValue(tokenValue);

        //Assert
        assertThat(tokenHash).isNotNull();
        assertThat(tokenHash).isNotEqualTo(tokenValue);
    }

}
