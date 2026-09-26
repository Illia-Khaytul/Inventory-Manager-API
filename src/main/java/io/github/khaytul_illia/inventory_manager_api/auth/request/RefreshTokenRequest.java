package io.github.khaytul_illia.inventory_manager_api.auth.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

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
    @Size(max = 50)
    String refreshToken

) {
}
