package io.github.khaytul_illia.inventory_manager_api.user.response;

import io.github.khaytul_illia.inventory_manager_api.user.User;

public record UserResponse(
    Long id,
    String username,
    User.UserRole role
) {
}
