package io.github.khaytul_illia.inventory_manager_api.product.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;
import org.hibernate.validator.constraints.Range;

import java.math.BigDecimal;

public record ProductFiltering(

    @Size(max = 100)
    String nameContains,

    @Range(min = 0, max = Integer.MAX_VALUE)
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
}
