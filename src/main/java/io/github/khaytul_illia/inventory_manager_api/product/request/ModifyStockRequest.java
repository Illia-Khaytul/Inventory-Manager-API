package io.github.khaytul_illia.inventory_manager_api.product.request;

import io.github.khaytul_illia.inventory_manager_api.common.validation.NotZero;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(
    description = "Product stock change amount used for the operation",
    example = """
        {
            "stockChange": 10
        }
        """
)
public record ModifyStockRequest(

    @NotNull
    @NotZero
    Integer stockChange

) {
}
