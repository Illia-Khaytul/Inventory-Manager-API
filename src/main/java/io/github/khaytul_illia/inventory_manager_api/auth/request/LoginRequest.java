package io.github.khaytul_illia.inventory_manager_api.auth.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(

    @NotBlank
    @Size(max = 50)
    String username,

    @NotBlank
    @Size(max = 50)
    String password

) {
}
