package io.github.khaytul_illia.inventory_manager_api.user.response;

import io.github.khaytul_illia.inventory_manager_api.user.User;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
    description = "Response containing user data",
    example = """
        {
            "id": 1,
            "username": "user1",
            "role": "CUSTOMER"
        }
        """
)
public record UserResponse(
    Long id,
    String username,
    User.UserRole role
) {
}
