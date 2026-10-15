package io.github.khaytul_illia.inventory_manager_api.user.request;

import io.github.khaytul_illia.inventory_manager_api.common.validation.SizeProperty;
import io.github.khaytul_illia.inventory_manager_api.common.validation.ValidationProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

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
    @SizeProperty(key = ValidationProperties.PropertyKey.USER_PASSWORD)
    String oldPassword,

    @NotBlank
    @SizeProperty(key = ValidationProperties.PropertyKey.USER_PASSWORD)
    String newPassword

) {
}
