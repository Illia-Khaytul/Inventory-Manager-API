package io.github.khaytul_illia.inventory_manager_api.support.utils.e2e;

import io.github.khaytul_illia.inventory_manager_api.auth.request.LoginRequest;
import io.github.khaytul_illia.inventory_manager_api.auth.request.RefreshTokenRequest;
import io.github.khaytul_illia.inventory_manager_api.auth.response.AccessTokenResponse;
import io.github.khaytul_illia.inventory_manager_api.error.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

public class AuthRequestUtils {

    private final RestTestClient restClient;
    private final ObjectMapper objectMapper;

    public AuthRequestUtils(RestTestClient restClient, ObjectMapper objectMapper) {
        this.restClient = restClient;
        this.objectMapper = objectMapper;
    }

    public AccessTokenResponse performSuccessfulLogin(LoginRequest request) {
        AccessTokenResponse response = restClient
            .post()
            .uri("/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .body(objectMapper.writeValueAsString(request))
            .exchange()
            .expectStatus().isOk()
            .returnResult(AccessTokenResponse.class).getResponseBody();

        assertThat(response).isNotNull();
        assertThat(response.subject()).isEqualTo(request.username());
        assertThat(response.accessToken()).isNotBlank();
        assertThat(response.refreshToken()).isNotBlank();

        return response;
    }

    public AccessTokenResponse performSuccessfulRefreshAccess(RefreshTokenRequest request, String subject, String accessToken) {
        AccessTokenResponse response = restClient
            .post()
            .uri("/auth/refresh-access")
            .contentType(MediaType.APPLICATION_JSON)
            .body(objectMapper.writeValueAsString(request))
            .exchange()
            .expectStatus().isOk()
            .returnResult(AccessTokenResponse.class).getResponseBody();

        assertThat(response).isNotNull();
        assertThat(response.subject()).isEqualTo(subject);

        String refreshAccessToken = response.accessToken();
        String refreshRefreshToken = response.refreshToken();

        assertThat(refreshAccessToken).isNotBlank();
        assertThat(refreshRefreshToken).isNotBlank();
        assertThat(refreshAccessToken).isNotEqualTo(accessToken);
        assertThat(refreshRefreshToken).isNotEqualTo(request.refreshToken());

        return response;
    }

    public void performErrorRefreshAccess(RefreshTokenRequest request, HttpStatus status, String errorMessage) {
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

    public void performLogout(RefreshTokenRequest request, String accessToken) {
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

    public void performLogoutAll(String accessToken) {
        restClient
            .post()
            .uri("/auth/logout-all")
            .header("Authorization", "Bearer " + accessToken)
            .exchange()
            .expectStatus().isNoContent()
            .expectBody(Void.class);
    }

}
