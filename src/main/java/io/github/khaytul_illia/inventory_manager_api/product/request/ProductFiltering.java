package io.github.khaytul_illia.inventory_manager_api.product.request;

import io.github.khaytul_illia.inventory_manager_api.common.validation.NullOrNotBlank;
import io.github.khaytul_illia.inventory_manager_api.common.validation.RangeProperty;
import io.github.khaytul_illia.inventory_manager_api.common.validation.SizeProperty;
import io.github.khaytul_illia.inventory_manager_api.common.validation.ValidationProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.beans.ConstructorProperties;
import java.math.BigDecimal;

@Schema(
    description = "Filtering data used for product filtering",
    example = """
        {
            "nameContains": " Product ",
            "minStock": null,
            "minPrice": 112,
            "maxPrice": 200
        }
        """
)
public record ProductFiltering(

    @NullOrNotBlank
    @SizeProperty(key = ValidationProperties.PropertyKey.PRODUCT_NAME)
    String nameContains,

    @PositiveOrZero
    Integer minStock,

    @RangeProperty(key = ValidationProperties.PropertyKey.PRODUCT_PRICE)
    BigDecimal minPrice,

    @RangeProperty(key = ValidationProperties.PropertyKey.PRODUCT_PRICE)
    BigDecimal maxPrice,

    boolean showDeleted

) {

    @ConstructorProperties({"name_contains", "min_stock", "min_price", "max_price", "show_deleted"})
    public ProductFiltering(String nameContains, Integer minStock, BigDecimal minPrice, BigDecimal maxPrice, boolean showDeleted){
        this.nameContains = nameContains;
        this.minStock = minStock;
        this.minPrice = minPrice;
        this.maxPrice = maxPrice;
        this.showDeleted = showDeleted;
    }

    public boolean isEmpty(){
        return (nameContains == null || nameContains.isBlank()) && minStock == null && minPrice == null && maxPrice == null && !showDeleted;
    }

}
