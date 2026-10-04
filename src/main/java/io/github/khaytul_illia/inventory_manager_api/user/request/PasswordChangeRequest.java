package io.github.khaytul_illia.inventory_manager_api.user.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(
    description = "New password data to change the old user password",
    example = """
        {
            "oldPassword": "old_pa55word",
            "newPassword": "new_pa55word"
        }
        """
)
public record PasswordChangeRequest(

    @NotBlank
    @Size(max = 50)
    String oldPassword,

    @NotBlank
    @Size(max = 50)
    String newPassword

) {
}
