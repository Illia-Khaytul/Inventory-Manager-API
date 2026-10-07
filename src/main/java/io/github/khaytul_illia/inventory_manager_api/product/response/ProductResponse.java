package io.github.khaytul_illia.inventory_manager_api.product.response;

import com.fasterxml.jackson.annotation.JsonView;
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
            "modifiedAt": "2026-10-074T13:30:00Z",
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
    @JsonView(OperatorView.class)
    Instant createdAt,
    @JsonView(OperatorView.class)
    String createdBy,
    @JsonView(OperatorView.class)
    Instant modifiedAt,
    @JsonView(OperatorView.class)
    String modifiedBy
) {

    public interface OperatorView{}

}
