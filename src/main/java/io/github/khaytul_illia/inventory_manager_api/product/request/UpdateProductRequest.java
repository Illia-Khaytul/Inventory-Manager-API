package io.github.khaytul_illia.inventory_manager_api.product.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.github.khaytul_illia.inventory_manager_api.common.validation.NullOrNotBlank;
import io.github.khaytul_illia.inventory_manager_api.common.validation.RangeProperty;
import io.github.khaytul_illia.inventory_manager_api.common.validation.SizeProperty;
import io.github.khaytul_illia.inventory_manager_api.common.validation.ValidationProperties;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(
    description = "Product data used for product updating",
    example = """
        {
            "name": "New Product Name",
            "description": "New more detailed description",
            "price": null
        }
        """
)
public record UpdateProductRequest(

    @NullOrNotBlank
    @SizeProperty(key = ValidationProperties.PropertyKey.PRODUCT_NAME)
    String name,

    @NullOrNotBlank
    @SizeProperty(key = ValidationProperties.PropertyKey.PRODUCT_DESCRIPTION)
    String description,

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    @RangeProperty(key = ValidationProperties.PropertyKey.PRODUCT_PRICE)
    BigDecimal price

) {

    public boolean isEmpty(){
        return name == null && description == null && price == null;
    }

}
