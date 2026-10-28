package io.github.khaytul_illia.inventory_manager_api.user;

import io.github.khaytul_illia.inventory_manager_api.support.context.E2ETestContext;
import io.github.khaytul_illia.inventory_manager_api.auth.request.LoginRequest;
import io.github.khaytul_illia.inventory_manager_api.auth.response.AccessTokenResponse;
import io.github.khaytul_illia.inventory_manager_api.support.utils.e2e.UserRequestUtils;
import io.github.khaytul_illia.inventory_manager_api.user.request.CreateUserRequest;
import io.github.khaytul_illia.inventory_manager_api.user.request.PasswordChangeRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("User management API end-to-end tests")
public class UserManagementE2EIT extends E2ETestContext {

    private UserRequestUtils requestUtils;

    @BeforeEach
    void beforeEach(){
        requestUtils = new UserRequestUtils(restClient, objectMapper);
    }

    @Test
    @DisplayName("User creates new customer, logs in, changes password, then deletes customer, new customer gets deleted")
    void createCustomer_login_changePassword_deleteUser_customerGetsDeleted(){
        //Create customer
        String customerUsername = username + "_different";
        CreateUserRequest createUserRequest = new CreateUserRequest(customerUsername, password);

        requestUtils.performSuccessfulCreateCustomer(createUserRequest);

        assertThat(userRepository.count()).isEqualTo(2);

        //Login
        LoginRequest loginRequest = new LoginRequest(customerUsername, password);

        AccessTokenResponse loginResponse = authRequestUtils.performSuccessfulLogin(loginRequest);
        String accessToken = loginResponse.accessToken();

        //Change password
        String newPassword = password + "_new";
        PasswordChangeRequest passwordChangeRequest = new PasswordChangeRequest(password, newPassword);

        requestUtils.performSuccessfulPasswordChange(passwordChangeRequest, accessToken);

        assertThat(userRepository.existsByUsername(customerUsername)).isTrue();

        //Delete user
        requestUtils.performSuccessfulDeleteUser(accessToken);

        assertThat(userRepository.existsByUsername(username)).isTrue();
        assertThat(userRepository.existsByUsername(customerUsername)).isFalse();
    }

    @Test
    @DisplayName("Operator logs in, creates a new operator, then deletes itself, the new operator remains")
    void operatorLogin_createOperator_deleteCurrentUser_newOperatorRemains(){
        //Login
        LoginRequest loginRequest = new LoginRequest(username, password);

        AccessTokenResponse loginResponse = authRequestUtils.performSuccessfulLogin(loginRequest);
        String accessToken = loginResponse.accessToken();

        //Create operator
        String operatorUsername = username + "_different";
        CreateUserRequest createUserRequest = new CreateUserRequest(operatorUsername, password);

        requestUtils.performSuccessfulCreateOperator(createUserRequest, accessToken);

        assertThat(userRepository.count()).isEqualTo(2);

        //Delete original user
        requestUtils.performSuccessfulDeleteUser(accessToken);

        assertThat(userRepository.existsByUsername(username)).isFalse();
        assertThat(userRepository.existsByUsername(operatorUsername)).isTrue();
    }

    @Test
    @DisplayName("Operator log in, deletes itself, then tries to change its own password, nothing happens, user already deleted")
    void operatorLogin_deleteUser_changePassword_nothingHappens(){
        //Login
        LoginRequest loginRequest = new LoginRequest(username, password);

        AccessTokenResponse loginResponse = authRequestUtils.performSuccessfulLogin(loginRequest);
        String accessToken = loginResponse.accessToken();

        //Delete user
        requestUtils.performSuccessfulDeleteUser(accessToken);

        assertThat(userRepository.count()).isEqualTo(0);

        //Change password
        PasswordChangeRequest passwordChangeRequest = new PasswordChangeRequest(password, password + "_new");

        requestUtils.performErrorPasswordChange(passwordChangeRequest, accessToken, HttpStatus.NOT_FOUND, String.format("Authenticated user '%s' does not exist", username));

        assertThat(userRepository.count()).isEqualTo(0);
    }

    @Test
    @DisplayName("Operator logs in, deletes itself, then deletes itself again, nothing happens, user already deleted")
    void operatorLogin_deleteUser_deleteUserAgain_nothingHappens(){
        //Login
        LoginRequest loginRequest = new LoginRequest(username, password);

        AccessTokenResponse loginResponse = authRequestUtils.performSuccessfulLogin(loginRequest);
        String accessToken = loginResponse.accessToken();

        //Delete user
        requestUtils.performSuccessfulDeleteUser(accessToken);

        assertThat(userRepository.count()).isEqualTo(0);

        //Delete user again
        requestUtils.performSuccessfulDeleteUser(accessToken);

        assertThat(userRepository.count()).isEqualTo(0);
    }

}
