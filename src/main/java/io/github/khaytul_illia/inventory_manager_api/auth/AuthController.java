package io.github.khaytul_illia.inventory_manager_api.auth;

import io.github.khaytul_illia.inventory_manager_api.auth.request.LoginRequest;
import io.github.khaytul_illia.inventory_manager_api.auth.request.RefreshTokenRequest;
import io.github.khaytul_illia.inventory_manager_api.auth.response.AccessTokenResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(path = "/auth")
public class AuthController {

    @PostMapping(path = "/login")
    @ResponseStatus(HttpStatus.OK)
    public AccessTokenResponse login(
        @RequestBody @Valid LoginRequest request
    ){
        return null;
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
