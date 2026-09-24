package io.github.khaytul_illia.inventory_manager_api.openapi;

import io.github.khaytul_illia.inventory_manager_api.auth.response.AccessTokenResponse;
import io.github.khaytul_illia.inventory_manager_api.error.ErrorResponse;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.responses.ApiResponse;
import org.springframework.http.HttpStatus;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static io.github.khaytul_illia.inventory_manager_api.openapi.OpenApiConfig.*;

public class AuthEndpointResponseProvider {

    public static Map<String, ApiResponse> provideAuthEndpointResponses(){
        Map<String, ApiResponse> responses = new HashMap<>();
        responses.putAll(provideLoginResponses());

        return responses.entrySet().stream().collect(Collectors.toMap(
            entry -> "auth_" + entry.getKey(),
            Map.Entry::getValue
        ));
    }

    private static Map<String, ApiResponse> provideLoginResponses() {
        ErrorResponse login400ResponseBlankCase = formatErrorResponse(
            new ErrorResponse(HttpStatus.BAD_REQUEST, "Invalid request parameters", Map.of(
                "username", "cannot be blank",
                "password", "cannot be blank"
            ))
        );
        ErrorResponse login400ResponseSizeCase = formatErrorResponse(
            new ErrorResponse(HttpStatus.BAD_REQUEST, "Invalid request parameters", Map.of(
                "username", "size must be between 0 and 50",
                "password", "size must be between 0 and 50"
            ))
        );
        ErrorResponse login401response = formatErrorResponse(
            new ErrorResponse(HttpStatus.UNAUTHORIZED, "Invalid credentials", Map.of())
        );
        ErrorResponse login403Response = formatErrorResponse(
            new ErrorResponse(HttpStatus.FORBIDDEN, "Maximum amount of user sessions opened (10)", Map.of())
        );

        return Map.of(
            "login_success",
            buildApiResponse(
                "AccessTokenResponse",
                "Successful login"
            ),
            "login_400",
            buildApiResponse(
                "ErrorResponse",
                "Invalid login request parameters",
                Map.of(
                    "Null or empty",
                    new Example().value(login400ResponseBlankCase).summary("Login request had null or blank values"),
                    "Invalid size",
                    new Example().value(login400ResponseSizeCase).summary("Login request had parameters of an invalid size")
                )
            ),
            "login_401",
            buildApiResponse(
                "ErrorResponse",
                "Failed to authenticate user with provided credentials",
                new Example().value(login401response)
            ),
            "login_403",
            buildApiResponse(
                "ErrorResponse",
                "User already opened a maximum number of sessions",
                new Example().value(login403Response)
            )
        );
    }

}
