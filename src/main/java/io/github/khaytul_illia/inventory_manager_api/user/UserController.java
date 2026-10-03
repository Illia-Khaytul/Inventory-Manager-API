package io.github.khaytul_illia.inventory_manager_api.user;

import io.github.khaytul_illia.inventory_manager_api.user.request.CreateUserRequest;
import io.github.khaytul_illia.inventory_manager_api.user.request.PasswordChangeRequest;
import io.github.khaytul_illia.inventory_manager_api.user.response.UserResponse;
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
    public UserResponse createCustomer(
        @RequestBody @Valid CreateUserRequest request
    ){
        return userService.createUser(request, User.UserRole.CUSTOMER);
    }

    @PostMapping(path = "/operators")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse createOperator(
        @RequestBody @Valid CreateUserRequest request
    ){
        return userService.createUser(request, User.UserRole.OPERATOR);
    }

    @PatchMapping(path = "/password/change")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(
        @RequestBody @Valid PasswordChangeRequest request
    ){

    }

    @DeleteMapping(path = "")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(){

    }

}
