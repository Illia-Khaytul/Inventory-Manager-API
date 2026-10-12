package io.github.khaytul_illia.inventory_manager_api.product.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.github.khaytul_illia.inventory_manager_api.common.validation.NullOrNotBlank;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

@Schema(
    description = "Product data used for new product creation",
    example = """
        {
            "name": "Original Product",
            "description": "This product is so awesome that...",
            "price": "20.05"
        }
        """
)
public record CreateProductRequest(

    @NotBlank
    @Size(max = 100)
    String name,

    @NullOrNotBlank
    @Size(max = 1000)
    String description,

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    @NotNull
    @PositiveOrZero
    @DecimalMax("2147483647")
    BigDecimal price

) {
}
