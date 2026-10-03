package io.github.khaytul_illia.inventory_manager_api.user.response;

public record UserResponse(
    Long id,
    String username,
    String role
) {
}
