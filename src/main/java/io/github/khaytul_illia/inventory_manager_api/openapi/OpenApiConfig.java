package io.github.khaytul_illia.inventory_manager_api.openapi;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("dev")
public class OpenApiConfig {

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
        return new Components()
            .addSecuritySchemes(
                "JWT authentication",
                new SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")
            );
    }

}
