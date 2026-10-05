package io.github.khaytul_illia.inventory_manager_api.product.request;

import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.Range;

public record ModifyStockRequest(

    @NotNull
    @Range(min = Integer.MIN_VALUE, max = Integer.MAX_VALUE)
    int stockChange

) {
}
