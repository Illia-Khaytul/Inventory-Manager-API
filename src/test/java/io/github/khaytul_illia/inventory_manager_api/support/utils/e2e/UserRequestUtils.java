package io.github.khaytul_illia.inventory_manager_api.support.utils.e2e;

import io.github.khaytul_illia.inventory_manager_api.error.ErrorResponse;
import io.github.khaytul_illia.inventory_manager_api.user.request.CreateUserRequest;
import io.github.khaytul_illia.inventory_manager_api.user.request.PasswordChangeRequest;
import io.github.khaytul_illia.inventory_manager_api.user.response.UserResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

public class UserRequestUtils {

    private final RestTestClient restClient;
    private final ObjectMapper objectMapper;

    public UserRequestUtils(RestTestClient restClient, ObjectMapper objectMapper) {
        this.restClient = restClient;
        this.objectMapper = objectMapper;
    }

    public UserResponse performSuccessfulCreateCustomer(CreateUserRequest request){
        UserResponse response = restClient
            .post()
            .uri("/users")
            .contentType(MediaType.APPLICATION_JSON)
            .body(objectMapper.writeValueAsString(request))
            .exchange()
            .expectStatus().isCreated()
            .returnResult(UserResponse.class).getResponseBody();

        assertThat(response).isNotNull();
        assertThat(response.username()).isEqualTo(request.username());

        return response;
    }

    public UserResponse performSuccessfulCreateOperator(CreateUserRequest request, String accessToken){
        UserResponse response = restClient
            .post()
            .uri("/users/operators")
            .header("Authorization", "Bearer " + accessToken)
            .contentType(MediaType.APPLICATION_JSON)
            .body(objectMapper.writeValueAsString(request))
            .exchange()
            .expectStatus().isCreated()
            .returnResult(UserResponse.class).getResponseBody();

        assertThat(response).isNotNull();
        assertThat(response.username()).isEqualTo(request.username());

        return response;
    }

    public void performSuccessfulPasswordChange(PasswordChangeRequest request, String accessToken){
        restClient
            .patch()
            .uri("/users/password/change")
            .header("Authorization", "Bearer " + accessToken)
            .contentType(MediaType.APPLICATION_JSON)
            .body(objectMapper.writeValueAsString(request))
            .exchange()
            .expectStatus().isNoContent()
            .expectBody(Void.class);
    }

    public void performErrorPasswordChange(PasswordChangeRequest request, String accessToken, HttpStatus status, String errorMessage){
        ErrorResponse response = restClient
            .patch()
            .uri("/users/password/change")
            .header("Authorization", "Bearer " + accessToken)
            .contentType(MediaType.APPLICATION_JSON)
            .body(objectMapper.writeValueAsString(request))
            .exchange()
            .expectStatus().isEqualTo(status.value())
            .returnResult(ErrorResponse.class).getResponseBody();

        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo(status.value());
        assertThat(response.message()).isEqualTo(errorMessage);
    }

    public void performSuccessfulDeleteUser(String accessToken){
        restClient
            .delete()
            .uri("/users")
            .header("Authorization", "Bearer " + accessToken)
            .exchange()
            .expectStatus().isNoContent()
            .returnResult(Void.class);
    }

}
