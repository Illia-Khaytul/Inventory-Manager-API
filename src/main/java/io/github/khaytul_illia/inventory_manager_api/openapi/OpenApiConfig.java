package io.github.khaytul_illia.inventory_manager_api.openapi;

import io.github.khaytul_illia.inventory_manager_api.auth.response.AccessTokenResponse;
import io.github.khaytul_illia.inventory_manager_api.error.ErrorResponse;
import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.core.converter.ResolvedSchema;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.time.Instant;
import java.util.Map;

@Configuration
@Profile("dev")
public class OpenApiConfig {

    public static final Instant fixedTime = Instant.parse("2026-09-24T10:33:00Z");

    @Bean
    public OpenAPI openApi(){
        return new OpenAPI()
            .info(assembleInfo())
            .components(assembleComponents());
    }

    private Info assembleInfo(){
        return new Info()
            .title("Inventory Manager API")
            .version("v1")
            .summary("A simple inventory management system for order and product management.")
            .description("""
                **Description:**
                
                This application exposes an API that allows customers to create and monitor the lifecycle of their product orders and operators to manage the products and their stock.
                
                The API utilizes JWT authentication. It exposes endpoints to allow users to perform an initial login and manage their open sessions, as well as manage their user accounts.
                
                **How to use:**
                
                To use the API, new users must do the following:
                1. Create a new user account
                    - Customers: Use the API endpoint to create a new account
                    - Operators: Have another operator create an account for you
                2. Perform the initial login to receive the JWT access token and a refresh token (access-refresh token pair)
                3. Put the received JWT into the 'Authorize' slot and begin using the API
                4. When the JWT expires, receive a new access-refresh token pair at the access refresh endpoint with the received refresh token
                5. Once finished, log out from your current session using the refresh token
                """);
    }

    private Components assembleComponents(){
        Components components = new Components()
            .addSecuritySchemes(
                "JWT authentication",
                new SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")
            );

        AuthEndpointResponseProvider.provideAuthEndpointResponses().forEach(components::addResponses);

        addSchema(components, ErrorResponse.class);
        addSchema(components, AccessTokenResponse.class);

        return components;
    }

    public static void addSchema(Components components, Class<?> schemaClass){
        ResolvedSchema resolvedSchema = ModelConverters.getInstance()
            .resolveAsResolvedSchema(new AnnotatedType(schemaClass).resolveAsRef(true));

        if (resolvedSchema.referencedSchemas != null) {
            resolvedSchema.referencedSchemas.forEach(components::addSchemas);
        }
    }

    public static ErrorResponse formatErrorResponse(ErrorResponse response){
        return new ErrorResponse(
            fixedTime,
            response.status(),
            response.message(),
            response.data()
        );
    }

    public static ApiResponse buildApiResponse(String schemaName, String description){
        return buildApiResponse(schemaName, description, Map.of());
    }

    public static ApiResponse buildApiResponse(String schemaName, String description, Example example){
        return buildApiResponse(schemaName, description, Map.of("default", example));
    }

    public static ApiResponse buildApiResponse(String schemaName, String description, Map<String, Example> examples) {
        MediaType mediaType = new MediaType()
            .schema(new Schema<>().$ref("#/components/schemas/" + schemaName));

        if(examples != null && !examples.isEmpty()) {
            examples.forEach(mediaType::addExamples);
        }

        return new ApiResponse()
            .description(description)
            .content(new Content()
                .addMediaType(
                    org.springframework.http.MediaType.APPLICATION_JSON_VALUE,
                    mediaType
                )
            );
    }

}
