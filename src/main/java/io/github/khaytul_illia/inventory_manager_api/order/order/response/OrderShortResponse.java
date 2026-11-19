package io.github.khaytul_illia.inventory_manager_api.order.order.response;

import com.fasterxml.jackson.annotation.JsonView;
import io.github.khaytul_illia.inventory_manager_api.common.response.view.ResponseViews;
import io.github.khaytul_illia.inventory_manager_api.order.order.Order;

import java.math.BigDecimal;

public record OrderShortResponse(
    Long id,
    Order.OrderStatus status,
    BigDecimal totalPrice,
    @JsonView(ResponseViews.OperatorView.class)
    Long customerId,
    int totalItems
) {
}
