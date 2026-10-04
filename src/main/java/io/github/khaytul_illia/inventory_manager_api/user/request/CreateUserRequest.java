package io.github.khaytul_illia.inventory_manager_api.user.request;

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
    @Size(max = 50)
    String username,

    @NotBlank
    @Size(max = 50)
    String password

) {
}
