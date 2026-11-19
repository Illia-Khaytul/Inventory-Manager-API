package io.github.khaytul_illia.inventory_manager_api.order.item.response;

import java.math.BigDecimal;

public record OrderItemChangeResponse(
    Long id,
    BigDecimal totalPrice,
    int totalItems,
    OrderItemResponse item
) {
}
