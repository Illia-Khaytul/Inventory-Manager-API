package io.github.khaytul_illia.inventory_manager_api.order.item.request;

import io.github.khaytul_illia.inventory_manager_api.common.validation.NotZero;
import jakarta.validation.constraints.NotNull;

public record ModifyQuantityRequest(

    @NotNull
    @NotZero
    Integer quantityChange

) {
}
