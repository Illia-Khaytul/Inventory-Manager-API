package io.github.khaytul_illia.inventory_manager_api.user;

import io.github.khaytul_illia.inventory_manager_api.user.request.CreateUserRequest;
import io.github.khaytul_illia.inventory_manager_api.user.request.PasswordChangeRequest;
import io.github.khaytul_illia.inventory_manager_api.user.response.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(path = "/users")
@Tag(
    name = "Users",
    description = "API for user management"
)
@SecurityRequirement(name = "JWT authentication")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping(path = "")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
        summary = "Create new user with CUSTOMER role",
        description = """
            Creates a new user with the provided credentials and a CUSTOMER role.
            - Returns with 201 Created on successful user creation.
            - Returns with 400 Bad Request if the create user request or the password are invalid.
            - Returns with 409 Conflict if the provided username is already taken.
            """,
        security = {}
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", ref = "#/components/responses/users_create_customer_success"),
        @ApiResponse(responseCode = "400", ref = "#/components/responses/users_create_user_400"),
        @ApiResponse(responseCode = "409", ref = "#/components/responses/users_create_user_409")
    })
    public UserResponse createCustomer(
        @RequestBody @Valid CreateUserRequest request
    ){
        return userService.createUser(request, User.UserRole.CUSTOMER);
    }

    @PostMapping(path = "/operators")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
        summary = "Create new user with OPERATOR role",
        description = """
            Creates a new user with the provided credentials and a OPERATOR role.
            - Returns with 201 Created on successful user creation.
            - Returns with 400 Bad Request if the create user request or the password are invalid.
            - Returns with 401 Unauthorized if user is not authenticated.
            - Returns with 403 Forbidden if the authenticated user is not an OPERATOR.
            - Returns with 409 Conflict if the provided username is already taken.
            
            Note: Requires authentication as OPERATOR.
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", ref = "#/components/responses/users_create_operator_success"),
        @ApiResponse(responseCode = "400", ref = "#/components/responses/users_create_user_400"),
        @ApiResponse(responseCode = "401", ref = "#/components/responses/users_create_operator_401"),
        @ApiResponse(responseCode = "403", ref = "#/components/responses/users_create_operator_403"),
        @ApiResponse(responseCode = "409", ref = "#/components/responses/users_create_user_409")
    })
    public UserResponse createOperator(
        @RequestBody @Valid CreateUserRequest request
    ){
        return userService.createUser(request, User.UserRole.OPERATOR);
    }

    @PatchMapping(path = "/password/change")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
        summary = "Change the password of the authenticated user",
        description = """
            Changes the password of the authenticated user for the provided new password.
            - Returns with 204 No Content on successful password change.
            - Returns with 400 Bad Request if the password change request or the new password are invalid.
            - Returns with 401 Unauthorized if user is not authenticated.
            - Returns with 404 Not Found if the authenticated user does not exist.
            - Returns with 409 Conflict if the authenticated user got deleted mid-operation.
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", ref = "#/components/responses/users_change_password_success"),
        @ApiResponse(responseCode = "400", ref = "#/components/responses/users_change_password_400"),
        @ApiResponse(responseCode = "401", ref = "#/components/responses/users_change_password_401"),
        @ApiResponse(responseCode = "404", ref = "#/components/responses/users_change_password_404"),
        @ApiResponse(responseCode = "409", ref = "#/components/responses/users_change_password_409")
    })
    public void changePassword(
        @RequestBody @Valid PasswordChangeRequest request
    ){
        userService.changePassword(request);
    }

    @DeleteMapping(path = "")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(){
        userService.deleteUser();
    }

}
