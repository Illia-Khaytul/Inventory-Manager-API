package io.github.khaytul_illia.inventory_manager_api.product.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.github.khaytul_illia.inventory_manager_api.common.validation.NullOrNotBlank;
import io.github.khaytul_illia.inventory_manager_api.common.validation.RangeProperty;
import io.github.khaytul_illia.inventory_manager_api.common.validation.SizeProperty;
import io.github.khaytul_illia.inventory_manager_api.common.validation.ValidationProperties;
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
    @SizeProperty(key = ValidationProperties.PropertyKey.PRODUCT_NAME)
    String name,

    @NullOrNotBlank
    @SizeProperty(key = ValidationProperties.PropertyKey.PRODUCT_DESCRIPTION)
    String description,

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    @NotNull
    @RangeProperty(key = ValidationProperties.PropertyKey.PRODUCT_PRICE)
    BigDecimal price

) {
}
