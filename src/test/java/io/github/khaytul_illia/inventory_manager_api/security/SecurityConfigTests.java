package io.github.khaytul_illia.inventory_manager_api.security;

import io.github.khaytul_illia.inventory_manager_api.DummyController;
import io.github.khaytul_illia.inventory_manager_api.error.ErrorResponse;
import io.github.khaytul_illia.inventory_manager_api.security.jwt.JwtAuthenticationErrorHandler;
import io.github.khaytul_illia.inventory_manager_api.security.jwt.JwtConfig;
import io.github.khaytul_illia.inventory_manager_api.security.jwt.JwtRsaPemKeyConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;

import java.io.UnsupportedEncodingException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DummyController.class)
@Import({
    SecurityConfig.class,
    JwtRsaPemKeyConfig.class,
    JwtConfig.class,
    JwtAuthenticationErrorHandler.class,
    AuthenticationErrorHandler.class,
    AuthorizationErrorHandler.class
})
@ActiveProfiles("tests")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("Security configuration tests")
public class SecurityConfigTests {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtEncoder jwtEncoder;
    @Autowired
    private RSAPrivateKey jwtPrivateKey;
    @Autowired
    private RSAPublicKey jwtPublicKey;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Should generate a valid authentication token with user details from the JWT")
    void shouldGenerateAuthenticationWithProvidedUserDetails() throws Exception {
        //Arrange
        String role = "CUSTOMER";
        Map<String, Object> claims = Map.of("claim1", "value1", "claim2", "value2", "roles", role);
        Jwt jwt = generateJwt(jwtEncoder, Instant.now(), SignatureAlgorithm.RS512, claims);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(jwt.getTokenValue());

        //Act and Assert
        mockMvc
            .perform(
                get("/dummy")
                    .headers(headers)
            )
            .andExpect(authenticated().withAuthentication(authentication -> {
                assertThat(authentication.isAuthenticated()).isTrue();
                assertThat(authentication.getName()).isEqualTo("user");
                assertThat(authentication.getAuthorities()).extracting(GrantedAuthority::getAuthority).contains("ROLE_" + role);

                Jwt principal = (Jwt) authentication.getPrincipal();
                assertThat(principal).isNotNull();

                Map<String, Object> receivedClaims = principal.getClaims();
                assertThat(receivedClaims).containsAllEntriesOf(claims);
            }));
    }

    @Test
    @DisplayName("Should return 200 OK when accessing with a valid JWT")
    void shouldReturn200_whenValidJwt() throws Exception {
        Jwt jwt = generateJwt(jwtEncoder, Instant.now(), SignatureAlgorithm.RS512);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(jwt.getTokenValue());

        //Act and Assert
        mockMvc
            .perform(
                get("/dummy")
                    .headers(headers)
            )
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should return 401 Unauthorized when accessing without a JWT")
    void shouldReturn401_whenNoJwt() throws Exception {
        //Act and Assert
        mockMvc
            .perform(
                get("/dummy")
            )
            .andExpect(status().isUnauthorized())
            .andExpect(result -> assertRegularErrorResponse(result, HttpStatus.UNAUTHORIZED, "Authentication required to access this resource"));
    }

    @ParameterizedTest
    @MethodSource("provideInvalidOrExpiredJwts")
    @DisplayName("Should return 401 Unauthorized when accessing with an invalid or expired JWT")
    void shouldReturn401_whenInvalidOrExpiredJwt(String jwt) throws Exception{
        //Arrange
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(jwt);

        //Act and Assert
        mockMvc
            .perform(
                get("/dummy")
                    .headers(headers)
            )
            .andExpect(status().isUnauthorized())
            .andExpect(result -> assertRegularErrorResponse(result, HttpStatus.UNAUTHORIZED, "Invalid or expired access token"));
    }

    @Test
    @DisplayName("Should return 401 Unauthorized when accessing with a malformed Bearer header")
    void shouldReturn401_whenMalformedBearerHeader() throws Exception {
        //Arrange
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer unsupported_jwt_character_{}");

        //Act and Assert
        mockMvc
            .perform(
                get("/dummy")
                    .headers(headers)
            )
            .andExpect(status().isUnauthorized())
            .andExpect(result -> assertRegularErrorResponse(result, HttpStatus.UNAUTHORIZED, "Access token cannot be resolved"));
    }

    /*
            Provider methods
     */

    Stream<Arguments> provideInvalidOrExpiredJwts(){
        return Stream.of(
            //JWT is invalid
            Arguments.of("fake_header.fake_payload.fake_signature"),
            //JWT payload has been tempered with
            Arguments.of(provideTamperedJwt()),
            //JWT is expired
            Arguments.of(generateJwt(jwtEncoder, Instant.now().minusSeconds(60 * 60 * 3), SignatureAlgorithm.RS512).getTokenValue()),
            //JWT is signed with a different algorithm
            Arguments.of(provideMismatchedSignatureAlgorithmJwt())
        );
    }

    private String provideTamperedJwt(){
        Jwt jwt = generateJwt(jwtEncoder, Instant.now(), SignatureAlgorithm.RS512);

        String[] jwtParts = jwt.getTokenValue().split("\\.");
        String tamperedClaims = new String(Base64.getUrlDecoder().decode(jwtParts[1]))
            .replace("\"user\"", "\"fake_user\"");
        jwtParts[1] = Base64.getUrlEncoder().withoutPadding().encodeToString(tamperedClaims.getBytes());

        return String.join(".", jwtParts);
    }

    private String provideMismatchedSignatureAlgorithmJwt(){
        JwtEncoder badEncoder = NimbusJwtEncoder
            .withKeyPair(jwtPublicKey, jwtPrivateKey)
            .algorithm(SignatureAlgorithm.RS256)
            .build();

        return generateJwt(badEncoder, Instant.now(), SignatureAlgorithm.RS256).getTokenValue();
    }

    /*
            Helper methods
     */

    private void assertErrorResponse(ErrorResponse response, HttpStatus status, String message){
        assertThat(response.timestamp()).isCloseTo(Instant.now(), within(1, ChronoUnit.MINUTES));
        assertThat(response.status()).isEqualTo(status.value());
        assertThat(response.message()).isEqualTo(message);
    }

    private void assertRegularErrorResponse(MvcResult result, HttpStatus status, String message) throws UnsupportedEncodingException {
        ErrorResponse response = deserializeErrorResponse(result);

        assertErrorResponse(response, status, message);
        assertThat(response.data()).isEmpty();
    }

    private ErrorResponse deserializeErrorResponse(MvcResult result) throws UnsupportedEncodingException {
        return objectMapper.readValue(result.getResponse().getContentAsString(), ErrorResponse.class);
    }

    private Jwt generateJwt(JwtEncoder jwtEncoder, Instant timestamp, SignatureAlgorithm algorithm){
        return generateJwt(jwtEncoder, timestamp, algorithm, Map.of());
    }

    private Jwt generateJwt(JwtEncoder jwtEncoder, Instant timestamp, SignatureAlgorithm algorithm, Map<String, Object> claims){
        JwtClaimsSet jwtClaims = JwtClaimsSet.builder()
            .issuer("Inventory Manager API")
            .issuedAt(timestamp)
            .expiresAt(timestamp.plusSeconds(60 * 15))
            .subject("user")
            .claims(map -> map.putAll(claims))
            .build();
        JwsHeader jwtHeader = JwsHeader.with(algorithm).build();
        JwtEncoderParameters jwtParameters = JwtEncoderParameters.from(jwtHeader, jwtClaims);

        return jwtEncoder.encode(jwtParameters);
    }

}