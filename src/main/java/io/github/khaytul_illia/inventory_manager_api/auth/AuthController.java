package io.github.khaytul_illia.inventory_manager_api.auth;

import io.github.khaytul_illia.inventory_manager_api.auth.request.LoginRequest;
import io.github.khaytul_illia.inventory_manager_api.auth.request.RefreshTokenRequest;
import io.github.khaytul_illia.inventory_manager_api.auth.response.AccessTokenResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(path = "/auth")
@Tag(
    name = "Auth",
    description = "API for login and session management"
)
@SecurityRequirement(name = "JWT authentication")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping(path = "/login")
    @ResponseStatus(HttpStatus.OK)
    public AccessTokenResponse login(
        @RequestBody @Valid LoginRequest request
    ){
        return authService.login(request);
    }

    @PostMapping(path = "/refresh-access")
    @ResponseStatus(HttpStatus.OK)
    public AccessTokenResponse refreshAccess(
        @RequestBody @Valid RefreshTokenRequest request
    ){
        return null;
    }

    @PostMapping(path = "/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(
        @RequestBody @Valid RefreshTokenRequest request
    ){

    }

    @PostMapping(path = "/logout-all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logoutAll(){

    }

}
