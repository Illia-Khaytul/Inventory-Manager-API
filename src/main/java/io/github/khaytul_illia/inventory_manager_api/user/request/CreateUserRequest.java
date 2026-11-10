package io.github.khaytul_illia.inventory_manager_api.user.request;

import io.github.khaytul_illia.inventory_manager_api.common.validation.SizeProperty;
import io.github.khaytul_illia.inventory_manager_api.common.validation.ValidationProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(
    description = "User credentials used for new user creation",
    example = """
        {
            "username": "user1",
            "password": "valid_pa55word"
        }
        """
)
public record CreateUserRequest(

    @NotBlank
    @SizeProperty(key = ValidationProperties.PropertyKey.USER_USERNAME)
    String username,

    @NotBlank
    @SizeProperty(key = ValidationProperties.PropertyKey.USER_PASSWORD)
    String password

) {
}
