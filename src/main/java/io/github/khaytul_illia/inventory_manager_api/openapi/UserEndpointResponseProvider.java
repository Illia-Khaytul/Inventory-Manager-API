package io.github.khaytul_illia.inventory_manager_api.openapi;

import io.github.khaytul_illia.inventory_manager_api.error.ErrorResponse;
import io.github.khaytul_illia.inventory_manager_api.user.User;
import io.github.khaytul_illia.inventory_manager_api.user.response.UserResponse;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.responses.ApiResponse;
import org.springframework.http.HttpStatus;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static io.github.khaytul_illia.inventory_manager_api.openapi.OpenApiConfig.buildApiResponse;
import static io.github.khaytul_illia.inventory_manager_api.openapi.OpenApiConfig.formatErrorResponse;

public class UserEndpointResponseProvider {

    public static Map<String, ApiResponse> provideUsersEndpointResponses(){
        Map<String, ApiResponse> responses = new HashMap<>();
        responses.putAll(provideCreateUserResponses());
        responses.putAll(provideCreateCustomerResponses());
        responses.putAll(provideCreateOperatorResponses());
        responses.putAll(provideChangePasswordResponses());
        responses.putAll(provideDeleteUserResponses());

        return responses.entrySet().stream().collect(Collectors.toMap(
            entry -> "users_" + entry.getKey(),
            Map.Entry::getValue
        ));
    }

    private static Map<String, ApiResponse> provideCreateUserResponses(){
        ErrorResponse createUser400ResponseBlankCase = formatErrorResponse(
            new ErrorResponse(HttpStatus.BAD_REQUEST, "Invalid request parameters", Map.of(
                "username", "cannot be blank",
                "password", "cannot be blank"
            ))
        );
        ErrorResponse createUser400ResponseSizeCase = formatErrorResponse(
            new ErrorResponse(HttpStatus.BAD_REQUEST, "Invalid request parameters", Map.of(
                "username", "size must be between 0 and 50",
                "password", "size must be between 0 and 50"
            ))
        );
        ErrorResponse createUser400ResponseInvalidPassword = formatErrorResponse(
            new ErrorResponse(HttpStatus.BAD_REQUEST, "Invalid password", Map.of(
                "errors", List.of(
                    "Password must be 6 or more characters in length.",
                    "Password must contain 2 or more digit characters.",
                    "Password contains a whitespace character."
                )
            ))
        );
        ErrorResponse createUser409Response = formatErrorResponse(
            new ErrorResponse(HttpStatus.CONFLICT, "Username 'user1' is not unique")
        );

        return Map.of(
            "create_user_400",
            buildApiResponse(
                "ErrorResponse",
                "Invalid create user request parameters or password value",
                Map.of(
                    "Null or empty",
                    new Example().value(createUser400ResponseBlankCase).description("Create user request had null or blank values"),
                    "Invalid size",
                    new Example().value(createUser400ResponseSizeCase).description("Create user request has parameters of an invalid size"),
                    "Invalid password",
                    new Example().value(createUser400ResponseInvalidPassword).description("Provided user password did not follow configured validation rules")
                )
            ),
            "create_user_409",
            buildApiResponse(
                "ErrorResponse",
                "Provided username is not unique",
                new Example().value(createUser409Response)
            )
        );
    }

    private static Map<String, ApiResponse> provideCreateCustomerResponses(){
        UserResponse createCustomerSuccessResponse = new UserResponse(1L, "user1", User.UserRole.CUSTOMER);

        return Map.of(
            "create_customer_success",
            buildApiResponse(
                "UserResponse",
                "Successfully created CUSTOMER user",
                new Example().value(createCustomerSuccessResponse)
            )
        );
    }

    private static Map<String, ApiResponse> provideCreateOperatorResponses(){
        UserResponse createOperatorSuccessResponse = new UserResponse(1L, "operator1", User.UserRole.OPERATOR);
        ErrorResponse createOperator401Response = formatErrorResponse(
            new ErrorResponse(HttpStatus.UNAUTHORIZED, "Authentication required to access this resource")
        );
        ErrorResponse createOperator403Response = formatErrorResponse(
            new ErrorResponse(HttpStatus.FORBIDDEN, "Forbidden from accessing this resource")
        );

        return Map.of(
            "create_operator_success",
            buildApiResponse(
                "UserResponse",
                "Successfully created OPERATOR user",
                new Example().value(createOperatorSuccessResponse)
            ),
            "create_operator_401",
            buildApiResponse(
                "ErrorResponse",
                "Accessing without authentication",
                new Example().value(createOperator401Response)
            ),
            "create_operator_403",
            buildApiResponse(
                "ErrorResponse",
                "Accessing with authentication but not an OPERATOR",
                new Example().value(createOperator403Response)
            )
        );
    }

    private static Map<String, ApiResponse> provideChangePasswordResponses(){
        ErrorResponse changePassword400ResponseBlankCase = formatErrorResponse(
            new ErrorResponse(HttpStatus.BAD_REQUEST, "Invalid request parameters", Map.of(
                "oldPassword", "cannot be blank",
                "newPassword", "cannot be blank"
            ))
        );
        ErrorResponse changePassword400ResponseSizeCase = formatErrorResponse(
            new ErrorResponse(HttpStatus.BAD_REQUEST, "Invalid request parameters", Map.of(
                "oldPassword", "size must be between 0 and 50",
                "newPassword", "size must be between 0 and 50"
            ))
        );
        ErrorResponse changePassword400ResponseInvalidPassword = formatErrorResponse(
            new ErrorResponse(HttpStatus.BAD_REQUEST, "Invalid password", Map.of(
                "errors", List.of(
                    "Password must be 6 or more characters in length.",
                    "Password must contain 2 or more digit characters.",
                    "Password contains a whitespace character."
                )
            ))
        );
        ErrorResponse changePassword401Response = formatErrorResponse(
            new ErrorResponse(HttpStatus.UNAUTHORIZED, "Authentication required to access this resource")
        );
        ErrorResponse changePassword404Response = formatErrorResponse(
            new ErrorResponse(HttpStatus.NOT_FOUND, "Authenticated user 'username' does not exist")
        );
        ErrorResponse changePassword409Response = formatErrorResponse(
            new ErrorResponse(HttpStatus.CONFLICT, "Concurrent modification error")
        );

        return Map.of(
            "change_password_success",
            new ApiResponse().description("Successfully changed user password"),
            "change_password_400",
            buildApiResponse(
                "ErrorResponse",
                "Invalid password change request parameters or new password value",
                Map.of(
                    "Null or empty",
                    new Example().value(changePassword400ResponseBlankCase).description("Password change request had null or blank values"),
                    "Invalid size",
                    new Example().value(changePassword400ResponseSizeCase).description("Password change request has parameters of an invalid size"),
                    "Invalid password",
                    new Example().value(changePassword400ResponseInvalidPassword).description("Provided new user password did not follow configured validation rules")
                )
            ),
            "change_password_401",
            buildApiResponse(
                "ErrorResponse",
                "Accessing without authentication",
                new Example().value(changePassword401Response)
            ),
            "change_password_404",
            buildApiResponse(
                "ErrorResponse",
                "Authenticated user does not exist",
                new Example().value(changePassword404Response)
            ),
            "change_password_409",
            buildApiResponse(
                "ErrorResponse",
                "Authenticated user got deleted concurrently",
                new Example().value(changePassword409Response)
            )
        );
    }

    private static Map<String, ApiResponse> provideDeleteUserResponses(){
        ErrorResponse deleteUser401Response = formatErrorResponse(
            new ErrorResponse(HttpStatus.UNAUTHORIZED, "Authentication required to access this resource")
        );

        return Map.of(
            "delete_user_success",
            new ApiResponse().description("Successfully deleted user"),
            "delete_user_401",
            buildApiResponse(
                "ErrorResponse",
                "Accessing without authentication",
                new Example().value(deleteUser401Response)
            )
        );
    }

}
