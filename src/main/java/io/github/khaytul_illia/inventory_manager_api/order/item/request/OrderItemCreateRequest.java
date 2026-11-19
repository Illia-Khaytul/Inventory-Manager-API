package io.github.khaytul_illia.inventory_manager_api.order.item.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record OrderItemCreateRequest(

    @NotNull
    @Positive
    Long productId,

    @NotNull
    @Positive
    Integer quantity

) {
}
