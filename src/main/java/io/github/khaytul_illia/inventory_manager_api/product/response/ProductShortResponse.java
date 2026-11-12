package io.github.khaytul_illia.inventory_manager_api.product.response;

import com.fasterxml.jackson.annotation.JsonView;
import io.github.khaytul_illia.inventory_manager_api.common.response.view.ResponseViews;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;

@Schema(
    description = "Response containing reduced product data for paginated responses",
    example = """
        {
            "id": 1,
            "name": "Original Product",
            "stock": 100,
            "price": 20.05
        }
        """
)
public record ProductShortResponse(
    Long id,
    String name,
    int stock,
    BigDecimal price,
    @JsonView(ResponseViews.OperatorView.class)
    boolean deleted
) {
}
