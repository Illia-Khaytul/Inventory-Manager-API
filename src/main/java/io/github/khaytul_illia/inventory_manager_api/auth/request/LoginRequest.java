package io.github.khaytul_illia.inventory_manager_api.auth.request;

import io.github.khaytul_illia.inventory_manager_api.common.validation.SizeProperty;
import io.github.khaytul_illia.inventory_manager_api.common.validation.ValidationProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(
    description = "User credentials used for authentication during login",
    example = """
        {
            "username": "user1",
            "password": "user_password"
        }
        """
)
public record LoginRequest(

    @NotBlank
    @SizeProperty(key = ValidationProperties.PropertyKey.USER_USERNAME)
    String username,

    @NotBlank
    @SizeProperty(key = ValidationProperties.PropertyKey.USER_PASSWORD)
    String password

) {
}
