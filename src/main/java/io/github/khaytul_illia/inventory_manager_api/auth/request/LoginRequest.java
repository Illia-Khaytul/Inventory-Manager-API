package io.github.khaytul_illia.inventory_manager_api.auth.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

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
    @Size(max = 50)
    String username,

    @NotBlank
    @Size(max = 50)
    String password

) {
}
