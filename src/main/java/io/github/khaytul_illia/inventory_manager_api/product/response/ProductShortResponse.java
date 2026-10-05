package io.github.khaytul_illia.inventory_manager_api.product.response;

import java.math.BigDecimal;

public record ProductShortResponse(
    long id,
    String name,
    int stock,
    BigDecimal price
) {
}
