package io.github.khaytul_illia.inventory_manager_api.auth.token;

import io.github.khaytul_illia.inventory_manager_api.auth.session.UserSession;
import io.github.khaytul_illia.inventory_manager_api.support.data.TestDataConstants;
import io.github.khaytul_illia.inventory_manager_api.support.data.UserSessionTestDataFactory;
import io.github.khaytul_illia.inventory_manager_api.support.data.UserTestDataFactory;
import io.github.khaytul_illia.inventory_manager_api.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.*;

import java.time.Instant;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserSessionFactory tests")
public class TokenFactoryTests {

    private final int accessTokenLifetime = 900;
    private final String accessTokenIssuer = "http://localhost:8080/api/v1";
    @Mock
    private JwtEncoder jwtEncoder;
    private TokenFactory tokenFactory;

    @BeforeEach
    void beforeEach(){
        tokenFactory = new TokenFactory(
            accessTokenLifetime,
            accessTokenIssuer,
            jwtEncoder
        );
    }

    @ParameterizedTest
    @MethodSource("provideRefreshTokenBuildParameters")
    @DisplayName("Should build RefreshToken entity")
    void shouldBuildRefreshToken(String tokenHash, UserSession session){
        //Arrange
        String tokenValue = TestDataConstants.REFRESH_TOKEN_VALUE;

        TokenFactory tokenFactorySpy = spy(tokenFactory);
        doReturn(tokenHash)
            .when(tokenFactorySpy).hashTokenValue(tokenValue);

        //Act
        RefreshToken refreshToken = tokenFactorySpy.buildRefreshToken(tokenValue, session.getCreatedAt(), session);

        //Assert
        assertThat(refreshToken).isNotNull();
        assertThat(refreshToken.getId()).isNull();
        assertThat(refreshToken.getTokenValue()).isEqualTo(tokenHash);
        assertThat(refreshToken.getIssuedAt()).isEqualTo(session.getCreatedAt());
        assertThat(refreshToken.isUsed()).isFalse();
        assertThat(refreshToken.getSession()).isEqualTo(session);
        assertThat(refreshToken.getVersion()).isNull();

        verify(tokenFactorySpy).hashTokenValue(tokenValue);
    }

    @ParameterizedTest
    @MethodSource("provideAccessTokenBuildParameters")
    @DisplayName("Should build JWT access token")
    void shouldBuildAccessToken(Instant issuedAt, User user){
        //Arrange
        Jwt generatedJwt = mock(Jwt.class);

        when(jwtEncoder.encode(any(JwtEncoderParameters.class)))
            .thenReturn(generatedJwt);

        //Act
        Jwt jwt = tokenFactory.buildAccessToken(issuedAt, user);

        //Assert
        assertThat(jwt).isNotNull();
        assertThat(jwt).isEqualTo(generatedJwt);

        ArgumentCaptor<JwtEncoderParameters> paramsCaptor = ArgumentCaptor.forClass(JwtEncoderParameters.class);
        verify(jwtEncoder).encode(paramsCaptor.capture());

        JwtEncoderParameters jwtParameters = paramsCaptor.getValue();

        JwsHeader jwtHeader = jwtParameters.getJwsHeader();
        assertThat(jwtHeader).isNotNull();
        assertThat(jwtHeader.getAlgorithm()).isEqualTo(SignatureAlgorithm.RS512);

        JwtClaimsSet jwtClaims = jwtParameters.getClaims();
        assertThat(jwtClaims.getIssuer().toString()).isEqualTo(accessTokenIssuer);
        assertThat(jwtClaims.getIssuedAt()).isEqualTo(issuedAt);
        assertThat(jwtClaims.getExpiresAt()).isEqualTo(issuedAt.plusSeconds(accessTokenLifetime));
        assertThat(jwtClaims.getSubject()).isEqualTo(user.getUsername());
        assertThat(jwtClaims.getClaimAsString("roles")).isEqualTo(user.getRole().name());
    }

    @ParameterizedTest
    @ValueSource(strings = {"token_value_1", "different_token"})
    @DisplayName("Should return hash of the provided token value")
    void shouldHashTokenValue(String tokenValue){
        //Act
        String tokenHash = tokenFactory.hashTokenValue(tokenValue);

        //Assert
        assertThat(tokenHash).isNotNull();
        assertThat(tokenHash).isNotEqualTo(tokenValue);
    }

    /*
            Test data provider methods
     */

    static Stream<Arguments> provideRefreshTokenBuildParameters(){
        User user = UserTestDataFactory.existingDefaultUser();

        return Stream.of(
            Arguments.of(
                TestDataConstants.REFRESH_TOKEN_VALUE + "_1",
                UserSessionTestDataFactory.existingUserSession(true, Instant.parse("2026-10-01T18:20:00Z"), user)
            ),
            Arguments.of(
                TestDataConstants.REFRESH_TOKEN_VALUE + "_2",
                UserSessionTestDataFactory.existingUserSession(true, Instant.parse("2026-10-11T12:56:00Z"), user)
            )
        );
    }

    static Stream<Arguments> provideAccessTokenBuildParameters(){
        return Stream.of(
            Arguments.of(
                Instant.parse("2026-10-01T18:20:00Z"),
                UserTestDataFactory.existingDefaultUser()
            ),
            Arguments.of(
                Instant.parse("2026-10-11T12:56:00Z"),
                UserTestDataFactory.existingDefaultOperator()
            )
        );
    }

}
