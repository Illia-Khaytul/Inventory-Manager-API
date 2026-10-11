package io.github.khaytul_illia.inventory_manager_api.product.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(
    description = "Response containing reduced product data for paginated responses",
    example = """
        {
            "id": 1,
            "name": "Original Product",
            "stock": 100,
            "price": "20.05"
        }
        """
)
public record ProductShortResponse(
    Long id,
    String name,
    int stock,
    BigDecimal price
) {
}
