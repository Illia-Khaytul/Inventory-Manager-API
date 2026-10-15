package io.github.khaytul_illia.inventory_manager_api.auth.request;

import io.github.khaytul_illia.inventory_manager_api.common.validation.SizeProperty;
import io.github.khaytul_illia.inventory_manager_api.common.validation.ValidationProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(
    description = "Refresh token used to renew the access to the API",
    example = """
        {
            "refreshToken": "e58ed763-928c-4155-bee9-fdbaaadc15f3"
        }
        """
)
public record RefreshTokenRequest(

    @NotBlank
    @SizeProperty(key = ValidationProperties.PropertyKey.REFRESH_TOKEN)
    String refreshToken

) {
}
