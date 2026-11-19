package io.github.khaytul_illia.inventory_manager_api.order.item.response;

import java.math.BigDecimal;

public record OrderItemResponse(
    BigDecimal totalPrice,
    int quantity,
    int reserved,
    String productName,
    BigDecimal productPrice,
    Long productId
) {
}
