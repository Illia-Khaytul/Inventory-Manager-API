package io.github.khaytul_illia.inventory_manager_api.user.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PasswordChangeRequest(

    @NotBlank
    @Size(max = 50)
    String oldPassword,

    @NotBlank
    @Size(max = 50)
    String newPassword

) {
}
