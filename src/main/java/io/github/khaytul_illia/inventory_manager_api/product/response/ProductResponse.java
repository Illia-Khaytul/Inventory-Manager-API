package io.github.khaytul_illia.inventory_manager_api.product.response;

import com.fasterxml.jackson.annotation.JsonView;
import io.github.khaytul_illia.inventory_manager_api.common.response.view.ResponseViews;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;

@Schema(
    description = "Response containing product data",
    example = """
        {
            "id": 1,
            "name": "Original Product",
            "description": "This product is so awesome that...",
            "stock": 100,
            "price": "20.05",
            "createdAt": "2026-10-05T08:58:00Z",
            "createdBy": "operator1",
            "modifiedAt": "2026-10-07T13:30:00Z",
            "modifiedBy": "operator2"
        }
        """
)
public record ProductResponse(
    Long id,
    String name,
    String description,
    int stock,
    BigDecimal price,
    @JsonView(ResponseViews.OperatorView.class)
    Instant createdAt,
    @JsonView(ResponseViews.OperatorView.class)
    String createdBy,
    @JsonView(ResponseViews.OperatorView.class)
    Instant modifiedAt,
    @JsonView(ResponseViews.OperatorView.class)
    String modifiedBy
) {
}
