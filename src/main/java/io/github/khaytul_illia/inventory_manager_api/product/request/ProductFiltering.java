package io.github.khaytul_illia.inventory_manager_api.product.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.github.khaytul_illia.inventory_manager_api.common.validation.NullOrNotBlank;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

@Schema(
    description = "Filtering data used for product filtering",
    example = """
        {
            "nameContains": " Product ",
            "minStock": null,
            "minPrice": "112",
            "maxPrice": "200"
        }
        """
)
public record ProductFiltering(

    @Size(max = 100)
    @NullOrNotBlank
    String nameContains,

    @PositiveOrZero
    Integer minStock,

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    @PositiveOrZero
    @DecimalMax("2147483647")
    BigDecimal minPrice,

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    @PositiveOrZero
    @DecimalMax("2147483647")
    BigDecimal maxPrice

) {

    public boolean isEmpty(){
        return (nameContains == null || nameContains.isBlank()) && minStock == null && minPrice == null && maxPrice == null;
    }

}
