package io.github.khaytul_illia.inventory_manager_api.user;

import io.github.khaytul_illia.inventory_manager_api.TestcontainersConfiguration;
import io.github.khaytul_illia.inventory_manager_api.auth.request.LoginRequest;
import io.github.khaytul_illia.inventory_manager_api.auth.response.AccessTokenResponse;
import io.github.khaytul_illia.inventory_manager_api.auth.session.UserSessionRepository;
import io.github.khaytul_illia.inventory_manager_api.auth.token.RefreshTokenRepository;
import io.github.khaytul_illia.inventory_manager_api.error.ErrorResponse;
import io.github.khaytul_illia.inventory_manager_api.user.request.CreateUserRequest;
import io.github.khaytul_illia.inventory_manager_api.user.request.PasswordChangeRequest;
import io.github.khaytul_illia.inventory_manager_api.user.response.UserResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.client.RestTestClient;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
@AutoConfigureRestTestClient
@ActiveProfiles("test")
@DisplayName("User management API end-to-end tests")
public class UserManagementE2EIT {

    @Autowired
    private RestTestClient restClient;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UserSessionRepository sessionRepository;
    @Autowired
    private RefreshTokenRepository refreshTokenRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private final String username = "username";
    private final String password = "valid_pa55word";

    @BeforeEach
    void beforeEach(){
        userRepository.save(new User(null, username, passwordEncoder.encode(password), User.UserRole.OPERATOR));
    }

    @AfterEach
    void afterEach(){
        refreshTokenRepository.deleteAll();
        sessionRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("User creates new customer, logs in, changes password, then deletes customer, new customer gets deleted")
    void createCustomer_login_changePassword_deleteUser_customerGetsDeleted(){
        //Create customer
        String customerUsername = username + "_different";
        CreateUserRequest createUserRequest = new CreateUserRequest(customerUsername, password);

        performSuccessfulCreateCustomer(createUserRequest);

        assertThat(userRepository.count()).isEqualTo(2);

        //Login
        LoginRequest loginRequest = new LoginRequest(customerUsername, password);

        AccessTokenResponse loginResponse = performSuccessfulLogin(loginRequest);
        String accessToken = loginResponse.accessToken();

        //Change password
        String newPassword = password + "_new";
        PasswordChangeRequest passwordChangeRequest = new PasswordChangeRequest(password, newPassword);

        performSuccessfulPasswordChange(passwordChangeRequest, accessToken);

        assertThat(userRepository.existsByUsername(customerUsername)).isTrue();

        //Delete user
        performSuccessfulDeleteUser(accessToken);

        assertThat(userRepository.existsByUsername(username)).isTrue();
        assertThat(userRepository.existsByUsername(customerUsername)).isFalse();
    }

    @Test
    @DisplayName("Operator logs in, creates a new operator, then deletes itself, the new operator remains")
    void operatorLogin_createOperator_deleteCurrentUser_newOperatorRemains(){
        //Login
        LoginRequest loginRequest = new LoginRequest(username, password);

        AccessTokenResponse loginResponse = performSuccessfulLogin(loginRequest);
        String accessToken = loginResponse.accessToken();

        //Create operator
        String operatorUsername = username + "_different";
        CreateUserRequest createUserRequest = new CreateUserRequest(operatorUsername, password);

        performSuccessfulCreateOperator(createUserRequest, accessToken);

        assertThat(userRepository.count()).isEqualTo(2);

        //Delete original user
        performSuccessfulDeleteUser(accessToken);

        assertThat(userRepository.existsByUsername(username)).isFalse();
        assertThat(userRepository.existsByUsername(operatorUsername)).isTrue();
    }

    @Test
    @DisplayName("Operator log in, deletes itself, then tries to change its own password, nothing happens, user already deleted")
    void operatorLogin_deleteUser_changePassword_nothingHappens(){
        //Login
        LoginRequest loginRequest = new LoginRequest(username, password);

        AccessTokenResponse loginResponse = performSuccessfulLogin(loginRequest);
        String accessToken = loginResponse.accessToken();

        //Delete user
        performSuccessfulDeleteUser(accessToken);

        assertThat(userRepository.count()).isEqualTo(0);

        //Change password
        PasswordChangeRequest passwordChangeRequest = new PasswordChangeRequest(password, password + "_new");

        performErrorPasswordChange(passwordChangeRequest, accessToken, HttpStatus.NOT_FOUND, String.format("Authenticated user '%s' does not exist", username));

        assertThat(userRepository.count()).isEqualTo(0);
    }

    @Test
    @DisplayName("Operator logs in, deletes itself, then deletes itself again, nothing happens, user already deleted")
    void operatorLogin_deleteUser_deleteUserAgain_nothingHappens(){
        //Login
        LoginRequest loginRequest = new LoginRequest(username, password);

        AccessTokenResponse loginResponse = performSuccessfulLogin(loginRequest);
        String accessToken = loginResponse.accessToken();

        //Delete user
        performSuccessfulDeleteUser(accessToken);

        assertThat(userRepository.count()).isEqualTo(0);

        //Delete user again
        performSuccessfulDeleteUser(accessToken);

        assertThat(userRepository.count()).isEqualTo(0);
    }

    /*
            Helper methods
     */

    private void performSuccessfulCreateCustomer(CreateUserRequest request){
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
    }

    private void performSuccessfulCreateOperator(CreateUserRequest request, String accessToken){
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
    }

    private void performSuccessfulPasswordChange(PasswordChangeRequest request, String accessToken){
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

    private void performErrorPasswordChange(PasswordChangeRequest request, String accessToken, HttpStatus status, String errorMessage){
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

    private void performSuccessfulDeleteUser(String accessToken){
        restClient
            .delete()
            .uri("/users")
            .header("Authorization", "Bearer " + accessToken)
            .exchange()
            .expectStatus().isNoContent()
            .returnResult(Void.class);
    }

    private AccessTokenResponse performSuccessfulLogin(LoginRequest request){
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

}
