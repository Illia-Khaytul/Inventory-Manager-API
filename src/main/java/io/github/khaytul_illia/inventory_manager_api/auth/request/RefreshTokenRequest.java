package io.github.khaytul_illia.inventory_manager_api.auth.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RefreshTokenRequest(

    @NotBlank
    @Size(max = 50)
    String refreshToken

) {
}
