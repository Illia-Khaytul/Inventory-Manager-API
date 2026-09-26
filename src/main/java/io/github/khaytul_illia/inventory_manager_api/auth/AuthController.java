package io.github.khaytul_illia.inventory_manager_api.auth;

import io.github.khaytul_illia.inventory_manager_api.auth.request.LoginRequest;
import io.github.khaytul_illia.inventory_manager_api.auth.request.RefreshTokenRequest;
import io.github.khaytul_illia.inventory_manager_api.auth.response.AccessTokenResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
    @Operation(
        summary = "Log in and receive access to the application",
        description = """
            Authenticates the user by the provided credentials and opens a new user session, returning access and refresh tokens.
            - Returns with 200 OK on successful login.
            - Returns with 400 Bad Request if the login request has invalid fields.
            - Returns with 401 Unauthorized if credential authentication fails.
            - Returns with 403 Forbidden if the user has already opened a maximum amount of sessions.
            """,
        security = {}
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", ref = "#/components/responses/auth_login_success"),
        @ApiResponse(responseCode = "400", ref = "#/components/responses/auth_login_400"),
        @ApiResponse(responseCode = "401", ref = "#/components/responses/auth_login_401"),
        @ApiResponse(responseCode = "403", ref = "#/components/responses/auth_login_403")
    })
    public AccessTokenResponse login(
        @RequestBody @Valid LoginRequest request
    ){
        return authService.login(request);
    }

    @PostMapping(path = "/refresh-access")
    @ResponseStatus(HttpStatus.OK)
    @Operation(
        summary = "Refresh access to an open user session",
        description = """
            Checks for provided refresh token reuse or session invalidation and marks it as used, then generates a new one for the same session and returns it with a new access token.
            - Returns with 200 OK on successful access refresh.
            - Returns with 400 Bad Request if the refresh token request has invalid fields.
            - Returns with 401 Unauthorized if the refresh token is used, or the session is invalid or expired.
            """,
        security = {}
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", ref = "#/components/responses/auth_refresh_access_success"),
        @ApiResponse(responseCode = "400", ref = "#/components/responses/auth_refresh_access_400"),
        @ApiResponse(responseCode = "401", ref = "#/components/responses/auth_refresh_access_401")
    })
    public AccessTokenResponse refreshAccess(
        @RequestBody @Valid RefreshTokenRequest request
    ){
        return authService.refreshAccess(request);
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
