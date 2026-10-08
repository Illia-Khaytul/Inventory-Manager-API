package io.github.khaytul_illia.inventory_manager_api.product.request;

import io.github.khaytul_illia.inventory_manager_api.common.validation.NotZero;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.Range;

public record ModifyStockRequest(

    @NotNull
    @NotZero
    @Range(min = Integer.MIN_VALUE, max = Integer.MAX_VALUE)
    int stockChange

) {
}
