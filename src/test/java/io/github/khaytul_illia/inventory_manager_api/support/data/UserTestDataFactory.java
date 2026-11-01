package io.github.khaytul_illia.inventory_manager_api.support.data;

import io.github.khaytul_illia.inventory_manager_api.auth.request.LoginRequest;
import io.github.khaytul_illia.inventory_manager_api.user.User;
import io.github.khaytul_illia.inventory_manager_api.user.request.CreateUserRequest;
import io.github.khaytul_illia.inventory_manager_api.user.request.PasswordChangeRequest;
import io.github.khaytul_illia.inventory_manager_api.user.response.UserResponse;

public class UserTestDataFactory {

    public static final LoginRequest DEFAULT_LOGIN_REQUEST = defaults().toLoginRequest();
    public static final CreateUserRequest DEFAULT_CREATE_REQUEST = defaults().toCreateRequest();
    public static final PasswordChangeRequest DEFAULT_PASSWORD_CHANGE_REQUEST = new PasswordChangeRequest(
        TestDataConstants.USER_PASSWORD, TestDataConstants.USER_PASSWORD + "_new"
    );
    public static final UserResponse DEFAULT_CUSTOMER_RESPONSE = defaults().id(1L).toResponse();

    public static User newUniqueUser(){
        return defaults().username().build();
    }

    public static User newDefaultUser(){
        return defaults().build();
    }

    public static User existingDefaultUser(){
        return defaults().id(1L).build();
    }

    public static User existingDefaultOperator(){
        return builder()
            .id(1L)
            .username(TestDataConstants.USER_USERNAME)
            .role(User.UserRole.OPERATOR)
            .build();
    }

    public static CreateUserRequest uniqueCreateUserRequest(){
        return defaults().username().toCreateRequest();
    }

    public static UserTestDataBuilder defaults(){
        return builder()
            .username(TestDataConstants.USER_USERNAME)
            .password(TestDataConstants.USER_PASSWORD)
            .role(User.UserRole.CUSTOMER);
    }

    public static UserTestDataBuilder builder(){
        return new UserTestDataBuilder();
    }

    public static class UserTestDataBuilder{

        private Long id;
        private String username;
        private String password;
        private User.UserRole role;

        public UserTestDataBuilder id(Long id) {
            this.id = id;

            return this;
        }

        public UserTestDataBuilder id() {
            this.id = TestDataConstants.uniqueValue.getAndIncrement();

            return this;
        }

        public UserTestDataBuilder username(String username) {
            this.username = username;

            return this;
        }

        public UserTestDataBuilder username() {
            this.username = TestDataConstants.USER_USERNAME + "_" + TestDataConstants.uniqueValue.getAndIncrement();

            return this;
        }

        public UserTestDataBuilder password(String password) {
            this.password = password;

            return this;
        }

        public UserTestDataBuilder role(User.UserRole role) {
            this.role = role;

            return this;
        }

        public User build(){
            return new User(id, username, password, role);
        }

        public UserResponse toResponse(){
            return new UserResponse(id, username, role);
        }

        public LoginRequest toLoginRequest(){
            return new LoginRequest(username, password);
        }
        
        public CreateUserRequest toCreateRequest(){
            return new CreateUserRequest(username, password);
        }

    }

}
